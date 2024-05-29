/*
 * Copyright (c) 2010-2024 Haifeng Li. All rights reserved.
 *
 * Smile is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Smile is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Smile.  If not, see <https://www.gnu.org/licenses/>.
 */
package smile.llm.llama;

import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.List;
import smile.deep.tensor.Index;
import smile.deep.tensor.Tensor;
import smile.llm.CompletionPrediction;

/**
 * LLaMA model specification.
 *
 * @author Haifeng Li
 */
public class Llama {
    /** The transformer model. */
    final Transformer model;
    /** The tokenizer. */
    final Tokenizer tokenizer;

    /**
     * Constructor.
     * @param model the transformer model.
     */
    public Llama(Transformer model, Tokenizer tokenizer) {
        this.model = model;
        this.tokenizer = tokenizer;
    }

    /**
     * Builds a Llama instance by initializing and loading a model checkpoint.
     * @param checkpoint the directory path of checkpoint files.
     * @param tokenizer the path of tokenizer file.
     * @param maxSeqLen the maximum sequence length for input text.
     * @param maxBatchSize the maximum batch size for inference.
     * @return an instance of Llama model.
     */
    public static Llama build(String checkpoint, String tokenizer, int maxBatchSize, int maxSeqLen) throws IOException {
        return build(checkpoint, tokenizer, maxBatchSize, maxSeqLen, 1);
    }

    /**
     * Builds a Llama instance by initializing and loading a model checkpoint.
     * @param checkpointDir the directory path of checkpoint files.
     * @param tokenizerPath the path of tokenizer file.
     * @param maxSeqLen the maximum sequence length for input text.
     * @param maxBatchSize the maximum batch size for inference.
     * @param modelParallelSize the number of model parallel processes.
     * @return an instance of Llama model.
     */
    public static Llama build(String checkpointDir, String tokenizerPath, int maxBatchSize, int maxSeqLen, int modelParallelSize) throws IOException {
        File dir = new File(checkpointDir);
        List<String> checkpoints = new ArrayList<>();
        for (var file : dir.listFiles()) {
            var path = file.getPath();
            if (path.endsWith(".pth")) {
                checkpoints.add(path);
            }
        }

        if (checkpoints.isEmpty()) {
            throw new IllegalArgumentException("No checkpoint files found in " + checkpointDir);
        }

        if (checkpoints.size() != modelParallelSize) {
            throw new IllegalStateException(String.format("Loading a checkpoint for MP=%d but world size is %d", checkpoints.size(), modelParallelSize));
        }

        var modelArgs = ModelArgs.from(checkpointDir + "/params.json", maxBatchSize, maxSeqLen);

        var tokenizer = Tokenizer.of(tokenizerPath);
        if (tokenizer.size() != modelArgs.vocabSize()) {
            throw new IllegalStateException("Tokenizer and ModelArgs have different vocabulary size.");
        }

        var model = new Transformer(modelArgs);

        Collections.sort(checkpoints);
        String rank = System.getenv("RANK");
        var checkpoint = checkpoints.get(Integer.valueOf(rank == null ? "0" : rank));
        model.load(checkpoint);
        return new Llama(model, tokenizer);
    }

    /**
     * Generates text sequences based on provided prompts. This method uses
     * the provided prompts as a basis for generating text. It employs nucleus
     * sampling to produce text with controlled randomness.
     * @param prompts List of tokenized prompts, where each prompt is represented as a list of integers.
     * @param maxGenLen Maximum length of the generated text sequence.
     * @param temperature Temperature value for controlling randomness in sampling.
     * @param topp Top-p probability threshold for nucleus sampling.
     * @param logprobs Flag indicating whether to compute token log probabilities.
     * @param echo Flag indicating whether to include prompt tokens in the generated output.
     * @return The generated text completion.
     */
    public CompletionPrediction[] generate(int[][] prompts, int maxGenLen, double temperature, double topp, boolean logprobs, boolean echo) {
        int batchSize = prompts.length;
        if (batchSize > model.params.maxBatchSize()) {
            throw new IllegalArgumentException("The number of prompts is greater than max_batch_size");
        }

        int minPromptLen = Integer.MAX_VALUE;
        int maxPromptLen = Integer.MIN_VALUE;
        for (var prompt : prompts) {
            minPromptLen = Math.min(minPromptLen, prompt.length);
            maxPromptLen = Math.max(maxPromptLen, prompt.length);
        }
        if (maxPromptLen > model.params.maxSeqLength()) {
            throw new IllegalArgumentException("The prompt length is greater than max_seq_len");
        }

        int totalLen = Math.min(model.params.maxSeqLength(), maxGenLen + maxPromptLen);

        int pad = tokenizer.pad();
        long[] shape = {batchSize, totalLen};
        Tensor tokens = Tensor.full(pad, shape);
        for (int i = 0; i < batchSize; i++) {
            tokens.put_(Tensor.of(prompts[i]), Index.of(i), Index.slice(0, prompts[i].length));
        }

        int prevPos = 0;
        Tensor eosReached = Tensor.of(new boolean[batchSize], batchSize);
        Tensor inputTextMask = tokens.ne(pad);

        if (minPromptLen == totalLen) {
            var logits = model.forward(tokens, prevPos);
            /*
            tokenLogprobs = -F.cross_entropy(
                input=logits.transpose(1, 2),
                target=tokens,
                reduction="none",
                ignore_index=pad_id,
                )
             */
        }

        Tensor stopTokens = Tensor.of(tokenizer.stopTokens());

        for (int curPos = minPromptLen; curPos < totalLen; curPos++) {
            var logits = model.forward(tokens.get(Index.Colon, Index.slice(prevPos, curPos)), prevPos);
            Tensor nextToken;
            if (temperature > 0) {
                var probs = logits.get(Index.Colon, Index.of(-1)).div(temperature).softmax(-1);
                nextToken = probs.topp(topp);
            } else {
                nextToken = logits.get(Index.Colon, Index.of(-1)).argmax(-1, false);
            }

            nextToken = nextToken.reshape(-1);
            // only replace token if prompt has already been generated
            nextToken = Tensor.where(inputTextMask.get(Index.Colon, Index.of(curPos)), tokens.get(Index.Colon, Index.of(curPos)), nextToken);
            tokens.put_(nextToken, Index.Colon, Index.of(curPos));

            if (logprobs) {
                /*
                tokenLogprobs[:,prev_pos + 1 :cur_pos + 1] = -Tensor.crossEntropy(
                        logits.transpose(1, 2),
                        tokens.get(Index.Colon, Index.slice(prevPos+1, curPos+1)),
                        "none",
                        ignore_index = pad_id,
                ); */
            }

            eosReached.or_(inputTextMask.get(Index.Colon, Index.of(curPos)).not().and(nextToken.isin(stopTokens)));
            prevPos = curPos;
            if (eosReached.all()) break;
        }

        CompletionPrediction[] predictions = new CompletionPrediction[batchSize];
        for (int i = 0; i < batchSize; i++) {
            // cut to max gen len
            int start = echo ? 0 : prompts[i].length;
            var toks = tokens.get(Index.of(i), Index.slice(start, prompts[i].length + maxGenLen)).intArray();
            //probs = tokenLogprobs.get(Index.of(i), Index.slice(start, prompts[i].length + maxGenLen);
            // cut to after eos tok if any
            for (var stopToken : tokenizer.stopTokens()) {
                for (int eosIdx = 0; eosIdx < toks.length; eosIdx++) {
                    if (toks[eosIdx] == stopToken) {
                        toks = Arrays.copyOf(toks, eosIdx);
                        // probs = probs[:eos_idx]if logprobs else None
                        break;
                    }
                }
            }

            predictions[i] = new CompletionPrediction(tokenizer.decode(toks));
        }
        return predictions;
    }

    /**
     * Performs text completion for a list of prompts
     * @param prompts List of text prompts.
     * @param maxGenLen Maximum length of the generated text sequence.
     * @param temperature Temperature value for controlling randomness in sampling.
     * @param topp Top-p probability threshold for nucleus sampling.
     * @param logprobs Flag indicating whether to compute token log probabilities.
     * @param echo Flag indicating whether to include prompt tokens in the generated output.
     * @return The generated text completion.
     */
    public CompletionPrediction[] complete(String[] prompts, int maxGenLen, double temperature, double topp, boolean logprobs, boolean echo) {
        int batchSize = prompts.length;
        int[][] tokens = new int[batchSize][];
        for (int i = 0; i < batchSize; i++) {
            tokens[i] = tokenizer.encode(prompts[i], true, false);
        }

        return generate(tokens, maxGenLen, temperature, topp, logprobs, echo);
    }

    /**
     * Generates assistant responses for a list of conversational dialogs.
     * @param dialogs List of conversational dialogs, where each dialog is a list of messages.
     * @param maxGenLen Maximum length of the generated text sequence.
     * @param temperature Temperature value for controlling randomness in sampling.
     * @param topp Top-p probability threshold for nucleus sampling.
     * @param logprobs Flag indicating whether to compute token log probabilities.
     * @return The generated chat responses.
     */
    public CompletionPrediction[] chat(Message[][] dialogs, int maxGenLen, double temperature, double topp, boolean logprobs) {
        int batchSize = dialogs.length;
        int[][] tokens = new int[batchSize][];
        for (int i = 0; i < batchSize; i++) {
            tokens[i] = tokenizer.encodeDialog(dialogs[i]);
        }

        return generate(tokens, maxGenLen, temperature, topp, logprobs, false);
    }
}

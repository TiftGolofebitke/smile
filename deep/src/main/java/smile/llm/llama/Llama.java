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
import org.bytedeco.cuda.global.cudart;
import org.bytedeco.pytorch.TypeMeta;
import org.bytedeco.pytorch.global.torch;
import org.bytedeco.pytorch.global.torch_cuda;
import smile.deep.tensor.Device;
import smile.deep.tensor.Index;
import smile.deep.tensor.ScalarType;
import smile.deep.tensor.Tensor;
import smile.llm.CompletionPrediction;
import smile.llm.Message;
import smile.util.AutoScope;

/**
 * LLaMA model specification.
 *
 * @author Haifeng Li
 */
public class Llama {
    private static final org.slf4j.Logger logger = org.slf4j.LoggerFactory.getLogger(Llama.class);
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
     * @param checkpointDir the directory path of checkpoint files.
     * @param tokenizerPath the path of tokenizer file.
     * @param maxSeqLen the maximum sequence length for input text.
     * @param maxBatchSize the maximum batch size for inference.
     * @return an instance of Llama model.
     */
    public static Llama build(String checkpointDir, String tokenizerPath, int maxBatchSize, int maxSeqLen) throws IOException {
        return build(checkpointDir, tokenizerPath, maxBatchSize, maxSeqLen, null);
    }

    /**
     * Builds a Llama instance by initializing and loading a model checkpoint.
     * @param checkpointDir the directory path of checkpoint files.
     * @param tokenizerPath the path of tokenizer file.
     * @param maxSeqLen the maximum sequence length for input text.
     * @param maxBatchSize the maximum batch size for inference.
     * @param seed the optional random number generation seed.
     * @return an instance of Llama model.
     */
    public static Llama build(String checkpointDir, String tokenizerPath, int maxBatchSize, int maxSeqLen, Long seed) throws IOException {
        String worldSize = Objects.requireNonNullElse(System.getenv("WORLD_SIZE"), "1");
        int modelParallelSize = Integer.valueOf(worldSize);
        String localRank = Objects.requireNonNullElse(System.getenv("LOCAL_RANK"), "0");
        byte rank = Byte.valueOf(localRank);

        var startTime = System.currentTimeMillis();
        cudart.cuInit(0);
        torch_cuda.set_device(rank);

        // half precision to lower memory usage.
        var meta = new TypeMeta();
        meta.put(Tensor.isBF16Supported() ? torch.ScalarType.BFloat16 : torch.ScalarType.Half);
        torch.set_default_dtype(meta);

        // seed must be the same in all processes
        if (seed != null) {
            torch.manual_seed(seed);
        }

        Device device = Device.CUDA(rank);
        var options = new Tensor.Options().device(device).requireGradients(false);
        Tensor.setDefaultOptions(options);
        var time = System.currentTimeMillis() - startTime;
        logger.info("Initialized CUDA[{}]: {}.{} seconds", rank, time/1000, time%1000);

        startTime = System.currentTimeMillis();
        File dir = new File(checkpointDir);
        List<String> checkpoints = new ArrayList<>();
        for (var file : dir.listFiles()) {
            var path = file.getPath();
            if (path.endsWith(".pt")) {
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

        var model = new Transformer(modelArgs, device);
        model.eval();
        Collections.sort(checkpoints);
        var checkpoint = checkpoints.get(rank);
        model.load(checkpoint);

        time = System.currentTimeMillis() - startTime;
        logger.info("Model {}[{}]: loaded in {}.{} seconds", checkpointDir, rank, time/1000, time%1000);
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

        try (var guard = Tensor.noGradGuard();
             var scope = new AutoScope()) {
            Tensor.push(scope);
            int totalLen = Math.min(model.params.maxSeqLength(), maxGenLen + maxPromptLen);

            int pad = tokenizer.pad();
            Tensor tokens = Tensor.full(pad, batchSize, totalLen);
            for (int i = 0; i < batchSize; i++) {
                var prompt = Tensor.of(prompts[i]);
                tokens.put_(prompt, Index.of(i), Index.slice(0, prompts[i].length));
            }

            Tensor tokenLogprobs = null;
            if (logprobs) {
                var options = new Tensor.Options().device(model.device).requireGradients(false).dtype(ScalarType.Float32);
                tokenLogprobs = Tensor.zeros(options, batchSize, totalLen);
            }

            Tensor eosReached = Tensor.of(new boolean[batchSize]);
            Tensor inputTextMask = tokens.ne(pad);
            Tensor stopTokens = Tensor.of(tokenizer.stopTokens());

            tokens = tokens.to(model.device);
            eosReached = eosReached.to(model.device);
            inputTextMask = inputTextMask.to(model.device);
            stopTokens = stopTokens.to(model.device);

            int prevPos = 0;
            if (minPromptLen == totalLen) {
                var logits = model.forward(tokens, prevPos);
                if (logprobs) {
                    tokenLogprobs = Tensor.crossEntropy(logits.transpose(1, 2), tokens, "none", pad).neg_();
                }
            }

            for (int curPos = minPromptLen; curPos < totalLen; curPos++) {
                try (var loopScope = new AutoScope()) {
                    Tensor.push(loopScope);
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
                    nextToken = Tensor.where(
                            inputTextMask.get(Index.Colon, Index.of(curPos)),
                            tokens.get(Index.Colon, Index.of(curPos)),
                            nextToken);
                    tokens.put_(nextToken, Index.Colon, Index.of(curPos));

                    if (logprobs) {
                        var entropy = Tensor.crossEntropy(
                                logits.transpose(1, 2),
                                tokens.get(Index.Colon, Index.slice(prevPos + 1, curPos + 1)),
                                "none", pad).neg_();
                        tokenLogprobs.put_(entropy, Index.Colon, Index.slice(prevPos + 1, curPos + 1));
                    }

                    var text = inputTextMask.get(Index.Colon, Index.of(curPos)).not();
                    var stop = nextToken.isin(stopTokens);
                    eosReached.or_(text.and_(stop));
                    prevPos = curPos;
                    // Free up memory at each iteration
                    Tensor.pop();
                }
                if (eosReached.all()) break;
            }

            var tokenArray = tokens.to(Device.CPU()).longArray();
            float[] logprobArray = null;
            if (logprobs) {
                logprobArray = tokenLogprobs.to(Device.CPU()).floatArray();
            }
            CompletionPrediction[] predictions = new CompletionPrediction[batchSize];
            for (int i = 0; i < batchSize; i++) {
                // cut to max gen len
                int start = echo ? 0 : prompts[i].length;
                var completion = Arrays.stream(tokenArray)
                        .skip(i * totalLen + start)
                        .mapToInt(x -> (int) x)
                        .limit(prompts[i].length + maxGenLen - start)
                        .toArray();

                float[] probs = null;
                if (logprobs) {
                    probs = Arrays.copyOfRange(logprobArray, i * totalLen + start, i * totalLen + prompts[i].length + maxGenLen);
                }

                // cut to after eos tok if any
                for (var stopToken : tokenizer.stopTokens()) {
                    for (int eosIdx = 0; eosIdx < completion.length; eosIdx++) {
                        if (completion[eosIdx] == stopToken) {
                            completion = Arrays.copyOf(completion, eosIdx);
                            if (logprobs) {
                                probs = Arrays.copyOf(probs, eosIdx);
                            }
                            break;
                        }
                    }
                }

                predictions[i] = new CompletionPrediction(tokenizer.decode(completion), prompts[i], completion, probs);
            }
            Tensor.pop();
            System.gc();
            return predictions;
        }
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

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

import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.File;
import java.io.IOException;

/**
 * LLaMA model hyperparameters.
 * @param dim the dimension of token embedding.
 * @param numLayers the number of transformer blocks.
 * @param numHeads the number of attention heads.
 * @param numKvHeads the number of key and value heads.
 * @param vocabSize the size of the vocabulary.
 * @param multipleOf make SwiGLU hidden layer size multiple of large power of 2.
 * @param ffnDimMultiplier the multiplier for the hidden dimension of the feedforward layers.
 * @param normEps the epsilon value used for numerical stability in normalization layers.
 * @param ropeTheta the theta parameter in rotary positional encoding.
 * @param maxBatchSize the maximum batch size.
 * @param maxSeqLength the maximum sequence length for input data.
 *
 * @author Haifeng Li
 */
public record ModelArgs(int dim,
                        int numLayers,
                        int numHeads,
                        Integer numKvHeads,
                        int vocabSize,
                        int multipleOf,
                        Double ffnDimMultiplier,
                        double normEps,
                        double ropeTheta,
                        int maxBatchSize,
                        int maxSeqLength) {

    /**
     * Constructor with default parameter values.
     */
    public ModelArgs() {
        this(4096, 32, 32, null, -1, 356, null, 1E-5, 500000, 32, 2048);
    }

    /**
     * Loads the model hyperparameters from a JSON file.
     * @param path the file path.
     * @param maxBatchSize the maximum batch size.
     * @param maxSeqLength the maximum sequence length for input data.
     * @return the model hyperparameters.
     */
    public static ModelArgs from(String path, int maxBatchSize, int maxSeqLength) throws IOException {
        ObjectMapper mapper = new ObjectMapper();
        var node = mapper.readTree(new File(path));
        return new ModelArgs(
                node.get("dim").asInt(),
                node.get("n_layers").asInt(),
                node.get("n_heads").asInt(),
                node.get("n_kv_heads").asInt(),
                node.get("vocab_size").asInt(),
                node.get("multiple_of").asInt(),
                node.get("ffn_dim_multiplier").asDouble(),
                node.get("norm_eps").asDouble(),
                node.get("rope_theta").asDouble(),
                maxBatchSize,
                maxSeqLength
        );
    }
}

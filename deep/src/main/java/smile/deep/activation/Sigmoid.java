/*
 * Copyright (c) 2010-2021 Haifeng Li. All rights reserved.
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

package smile.deep.activation;

import org.bytedeco.pytorch.global.torch;
import smile.deep.tensor.Tensor;

/**
 * Sigmoid activation function.
 *
 * @author Haifeng Li
 */
public class Sigmoid extends ActivationFunction {
    /**
     * Constructor.
     * @param inplace true if the operation executes in-place.
     */
    public Sigmoid(boolean inplace) {
        super("Sigmoid", inplace);
    }

    @Override
    public Tensor forward(Tensor input) {
        var x = input.asTorch();
        if (!isTraining() && inplace) {
            torch.sigmoid_(x);
            return input;
        } else {
            return new Tensor(torch.sigmoid(x));
        }
    }
}

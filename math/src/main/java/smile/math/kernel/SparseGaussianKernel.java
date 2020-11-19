/*******************************************************************************
 * Copyright (c) 2010-2020 Haifeng Li. All rights reserved.
 *
 * Smile is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation, either version 3 of
 * the License, or (at your option) any later version.
 *
 * Smile is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with Smile.  If not, see <https://www.gnu.org/licenses/>.
 ******************************************************************************/

package smile.math.kernel;

import smile.math.MathEx;
import smile.util.SparseArray;

/**
 * Gaussian kernel, also referred as RBF kernel or squared exponential kernel.
 * <p>
 * <pre>
 *     k(u, v) = e<sup>-||u-v||<sup>2</sup> / (2 * &sigma;<sup>2</sup>)</sup>
 * </pre>
 * where <code>&sigma; &gt; 0</code> is the scale parameter of the kernel.
 * <p>
 * The Gaussian kernel is a good choice for a great deal of applications,
 * although sometimes it is remarked as being over used.

 * @author Haifeng Li
 */
public class SparseGaussianKernel extends Gaussian implements MercerKernel<SparseArray> {
    /**
     * Constructor.
     * @param sigma The length scale of kernel.
     */
    public SparseGaussianKernel(double sigma) {
        super(sigma);
    }

    @Override
    public double k(SparseArray x, SparseArray y) {
        return k(MathEx.distance(x, y));
    }
}

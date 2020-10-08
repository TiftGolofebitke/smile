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

import java.io.Serializable;
import java.util.function.ToDoubleBiFunction;
import java.util.stream.IntStream;
import smile.math.blas.UPLO;
import smile.math.matrix.Matrix;

/**
 * A Mercer Kernel is a kernel that is positive semi-definite. When a kernel
 * is positive semi-definite, one may exploit the kernel trick, the idea of
 * implicitly mapping data to a high-dimensional feature space where some
 * linear algorithm is applied that works exclusively with inner products.
 * Assume we have some mapping &#934; from an input space X to a feature space H,
 * then a kernel <code>k(u, v) = &lt;&#934;(u), &#934;(v)&gt;</code> may be used
 * to define the inner product in feature space H.
 * <p>
 * Positive definiteness in the context of kernel functions also implies that
 * a kernel matrix created using a particular kernel is positive semi-definite.
 * A matrix is positive semi-definite if its associated eigenvalues are nonnegative.
 * 
 * @author Haifeng Li
 */
public interface MercerKernel<T> extends ToDoubleBiFunction<T,T>, Serializable {

    /**
     * Kernel function.
     */
    double k(T x, T y);

    /**
     * Kernel function.
     * This is simply for Scala convenience.
     */
    default double apply(T x, T y) {
        return k(x, y);
    }

    @Override
    default double applyAsDouble(T x, T y) {
        return k(x, y);
    }

    /**
     * Returns the kernel matrix.
     *
     * @param x samples.
     * @return the kernel matrix.
     */
    default Matrix K(T[] x) {
        int n = x.length;
        int N = n * (n - 1) / 2;
        Matrix K = new Matrix(n, n);
        IntStream.range(0, N).parallel().forEach(k -> {
            int j = n - 2 - (int) Math.floor(Math.sqrt(-8*k + 4*n*(n-1)-7)/2.0 - 0.5);
            int i = k + j + 1 - n*(n-1)/2 + (n-j)*((n-j)-1)/2;
            K.set(i, j, k(x[i], x[j]));
        });

        for (int i = 0; i < n; i++) {
            for (int j = i + 1; j < n; j++) {
                K.set(i, j, K.get(j, i));
            }
        }

        K.uplo(UPLO.LOWER);
        return K;
    }

    /**
     * Returns the kernel matrix.
     *
     * @param x samples.
     * @param y samples.
     * @return the kernel matrix.
     */
    default Matrix K(T[] x, T[] y) {
        int m = x.length;
        int n = y.length;
        Matrix K = new Matrix(m, n);
        IntStream.range(0, m).parallel().forEach(i -> {
            T xi = x[i];
            for (int j = 0; j < n; j++) {
                K.set(i, j, k(xi, y[j]));
            }
        });

        return K;
    }
}

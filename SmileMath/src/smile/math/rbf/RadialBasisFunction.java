/******************************************************************************
 *                   Confidential Proprietary                                 *
 *         (c) Copyright Haifeng Li 2011, All Rights Reserved                 *
 ******************************************************************************/

package smile.math.rbf;

import smile.math.Function;

/**
 * A radial basis function (RBF) is a real-valued function whose value depends
 * only on the distance from the origin, so that &phi;(x)=&phi;(||x||); or
 * alternatively on the distance from some other point c, called a center, so
 * that &phi;(x,c)=&phi;(||x-c||). Any function &phi; that satisfies the
 * property  is a radial function. The norm is usually Euclidean distance,
 * although other distance functions are also possible. For example by
 * using probability metric it is for some radial functions possible
 * to avoid problems with ill conditioning of the matrix solved to
 * determine coefficients w<sub>i</sub> (see below), since the ||x|| is always
 * greater than zero.
 * <p>
 * Sums of radial basis functions are typically used to approximate given
 * functions:
 * <p>
 * y(x) = &Sigma; w<sub>i</sub> &phi;(||x-c<sub>i</sub>||)
 * <p>
 * where the approximating function y(x) is represented as a sum of N radial
 * basis functions, each associated with a different center c<sub>i</sub>, and weighted
 * by an appropriate coefficient w<sub>i</sub>. The weights w<sub>i</sub> can
 * be estimated using the matrix methods of linear least squares, because
 * the approximating function is linear in the weights.
 * <p>
 * This approximation process can also be interpreted as a simple kind of neural
 * network and has been particularly used in time series prediction and control
 * of nonlinear systems exhibiting sufficiently simple chaotic behavior,
 * 3D reconstruction in computer graphics (for example, hierarchical RBF).
 *
 * @author Haifeng Li
 */
public interface RadialBasisFunction extends Function {

}

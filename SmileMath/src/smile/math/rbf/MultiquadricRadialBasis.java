/******************************************************************************
 *                   Confidential Proprietary                                 *
 *         (c) Copyright Haifeng Li 2011, All Rights Reserved                 *
 ******************************************************************************/
package smile.math.rbf;

/**
 * Multiquadric RBF. &phi;(r) = (r<sup>2</sup> + r<sup>2</sup><sub>0</sub>)<sup>1/2</sup>
 * where r<sub>0</sub> is a scale factor. Multiquadrics are said to be less
 * sensitive to the choice of r<sub>0</sub> than som other functional forms.
 * <p>
 * In general, r<sub>0</sub> should be larger than the typical separation of
 * points but smaller than the "outer scale" or feature size of the function
 * to interplate. There can be several orders of magnitude difference between
 * the interpolation accuracy with a good choice for r<sub>0</sub>, versus a
 * poor choice, so it is definitely worth some experimentation. One way to
 * experiment is to construct an RBF interpolator omitting one data point
 * at a time and measuring the interpolation error at the omitted point.
 *
 * @author Haifeng Li
 */
public class MultiquadricRadialBasis implements RadialBasisFunction {

    private double r02;

    /**
     * Constructor.
     */
    public MultiquadricRadialBasis() {
        this(1.0);
    }

    /**
     * Constructor.
     */
    public MultiquadricRadialBasis(double scale) {
        r02 = scale * scale;
    }

    @Override
    public double f(double r) {
        return Math.sqrt(r * r + r02);
    }
}

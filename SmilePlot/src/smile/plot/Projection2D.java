/******************************************************************************
 *                   Confidential Proprietary                                 *
 *         (c) Copyright Haifeng Li 2011, All Rights Reserved                 *
 ******************************************************************************/
package smile.plot;

/**
 * Project 2D logical coordinates to Java2D coordinates.
 *
 * @author Haifeng Li
 */
class Projection2D extends Projection {

    /**
     * Constructor.
     */
    public Projection2D(PlotCanvas canvas) {
        super(canvas);
    }

    @Override
    double[] baseCoordsScreenProjectionRatio(double[] xy) {
        double[] ratio = new double[2];
        ratio[0] = (xy[0] - canvas.base.lowerBound[0]) / (canvas.base.upperBound[0] - canvas.base.lowerBound[0]);
        ratio[1] = (xy[1] - canvas.base.lowerBound[1]) / (canvas.base.upperBound[1] - canvas.base.lowerBound[1]);
        return ratio;
    }

    /**
     * Project the screen coordinate back to the logical coordinates.
     * @param x the x of Java2D coordinate in the canvas.
     * @param y the y of Java2D coordinate in the canvas
     */
    public double[] inverseProjection(int x, int y) {
        double[] sc = new double[2];

        double ratio = (canvas.base.upperBound[0] - canvas.base.lowerBound[0]) / (canvas.getWidth() * (1 - 2 * canvas.margin));
        sc[0] = canvas.base.lowerBound[0] + ratio * (x - canvas.getWidth() * canvas.margin);

        ratio = (canvas.base.upperBound[1] - canvas.base.lowerBound[1]) / (canvas.getHeight() * (1 - 2 * canvas.margin));
        sc[1] = canvas.base.lowerBound[1] + ratio * (canvas.getHeight() * (1 - canvas.margin) - y);

        return sc;
    }
}

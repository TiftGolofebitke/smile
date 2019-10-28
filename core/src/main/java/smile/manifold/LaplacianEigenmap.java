/*******************************************************************************
 * Copyright (c) 2010-2019 Haifeng Li
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
 *******************************************************************************/

package smile.manifold;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Optional;
import smile.data.SparseDataset;
import smile.graph.Graph;
import smile.graph.Graph.Edge;
import smile.math.matrix.DenseMatrix;
import smile.math.matrix.EVD;
import smile.math.matrix.SparseMatrix;
import smile.netlib.ARPACK;
import smile.util.SparseArray;

/**
 * Laplacian Eigenmap. Using the notion of the Laplacian of the nearest
 * neighbor adjacency graph, Laplacian Eigenmap computes a low dimensional
 * representation of the dataset that optimally preserves local neighborhood
 * information in a certain sense. The representation map generated by the
 * algorithm may be viewed as a discrete approximation to a continuous map
 * that naturally arises from the geometry of the manifold.
 * <p>
 * The locality preserving character of the Laplacian Eigenmap algorithm makes
 * it relatively insensitive to outliers and noise. It is also not prone to
 * "short circuiting" as only the local distances are used.
 *
 * @see IsoMap
 * @see LLE
 * 
 * <h2>References</h2>
 * <ol>
 * <li> Mikhail Belkin and Partha Niyogi. Laplacian Eigenmaps and Spectral Techniques for Embedding and Clustering. NIPS, 2001. </li>
 * </ol>
 * 
 * @author Haifeng Li
 */
public class LaplacianEigenmap {
    private static final org.slf4j.Logger logger = org.slf4j.LoggerFactory.getLogger(LaplacianEigenmap.class);

    /**
     * The width of heat kernel.
     */
    private double t;
    /**
     * The original sample index.
     */
    private int[] index;
    /**
     * Coordinate matrix.
     */
    private double[][] coordinates;
    /**
     * Nearest neighbor graph.
     */
    private Graph graph;

    /**
     * Constructor with discrete weights.
     * @param index the original sample index.
     * @param coordinates the coordinates.
     * @param graph the nearest neighbor graph.
     */
    public LaplacianEigenmap(int[] index, double[][] coordinates, Graph graph) {
        this(-1, index, coordinates, graph);
    }

    /**
     * Constructor with Gaussian kernel.
     * @param t the width of heat kernel.
     * @param index the original sample index.
     * @param coordinates the coordinates.
     * @param graph the nearest neighbor graph.
     */
    public LaplacianEigenmap(double t, int[] index, double[][] coordinates, Graph graph) {
        this.t = t;
        this.index = index;
        this.coordinates = coordinates;
        this.graph = graph;
    }

    /**
     * Laplacian Eigenmaps with discrete weights.
     * @param data the dataset.
     * @param k k-nearest neighbor.
     */
    public static LaplacianEigenmap of(double[][] data, int k) {
        return of(data, k, 2, -1);
    }

    /**
     * Laplacian Eigenmap with Gaussian kernel.
     * @param data the dataset.
     * @param d the dimension of the manifold.
     * @param k k-nearest neighbor.
     * @param t the smooth/width parameter of heat kernel e<sup>-||x-y||<sup>2</sup> / t</sup>.
     * Non-positive value means discrete weights.
     */
    public static LaplacianEigenmap of(double[][] data, int k, int d, double t) {
        // Use largest connected component of nearest neighbor graph.
        Graph graph = NearestNeighborGraph.of(data, k, Optional.empty());
        NearestNeighborGraph nng = NearestNeighborGraph.largest(graph);

        int[] index = nng.index;
        int n = index.length;
        graph = nng.graph;

        double[] D = new double[n];
        double gamma = -1.0 / t;

        ArrayList<SparseArray> W = new ArrayList<>(n);
        for (int i = 0; i < n; i++) {
            SparseArray row = new SparseArray();
            Collection<Edge> edges = graph.getEdges(i);
            for (Edge edge : edges) {
                int j = edge.v2;
                if (i == j) j = edge.v1;

                double w = t <= 0 ? 1.0 : Math.exp(gamma * edge.weight * edge.weight);
                row.set(j, w);
                D[i] += w;
            }
            D[i] = 1 / Math.sqrt(D[i]);
            W.add(i, row);
        }

        for (int i = 0; i < n; i++) {
            SparseArray row = W.get(i);
            for (SparseArray.Entry e : row) {
                e.update(-D[i] * e.x * D[e.i]);
            }
            row.set(i, 1.0);
        }

        // Here L is actually I - D^(-1/2) * W * D^(-1/2)
        SparseMatrix L = SparseDataset.of(W, n).toMatrix();
        L.setSymmetric(true);

        // ARPACK may not find all needed eigen values for k = d + 1.
        // Set it to 10 * (d + 1) as a hack to NCV parameter of DSAUPD.
        // Our Lanczos class has no such issue.
        EVD eigen = ARPACK.eigen(L, Math.min(10*(d+1), n-1), "SM");

        DenseMatrix V = eigen.getEigenVectors();
        double[][] coordinates = new double[n][d];
        for (int j = d; --j >= 0; ) {
            double norm = 0.0;
            int c = V.ncols() - j - 2;
            for (int i = 0; i < n; i++) {
                double xi = V.get(i, c) * D[i];
                coordinates[i][j] = xi;
                norm += xi * xi;
            }

            norm = Math.sqrt(norm);
            for (int i = 0; i < n; i++) {
                coordinates[i][j] /= norm;
            }
        }

        return new LaplacianEigenmap(t, index, coordinates, graph);
    }

    /**
     * Returns the original sample index. Because Laplacian Eigenmap is applied to the largest
     * connected component of k-nearest neighbor graph, we record the the original
     * indices of samples in the largest component.
     */
    public int[] getIndex() {
        return index;
    }

    /**
     * Returns the coordinates of projected data.
     */
    public double[][] getCoordinates() {
        return coordinates;
    }

    /**
     * Returns the nearest neighbor graph.
     */
    public Graph getNearestNeighborGraph() {
        return graph;
    }

    /**
     * Returns the width of heat kernel.
     */
    public double getHeatKernelWidth() {
        return t;
    }
}

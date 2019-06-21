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

package smile.classification;

import smile.math.distance.EuclideanDistance;
import smile.math.rbf.RadialBasisFunction;
import smile.data.NominalAttribute;
import smile.data.parser.DelimitedTextParser;
import smile.data.AttributeDataset;
import smile.data.parser.ArffParser;

import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import smile.math.MathEx;
import smile.math.rbf.GaussianRadialBasis;
import smile.util.SmileUtils;
import smile.validation.LOOCV;
import static org.junit.Assert.*;

/**
 *
 * @author Haifeng Li
 */
@SuppressWarnings("unused")
public class RBFNetworkTest {

    public RBFNetworkTest() {
    }

    @BeforeClass
    public static void setUpClass() throws Exception {
    }

    @AfterClass
    public static void tearDownClass() throws Exception {
    }

    @Before
    public void setUp() {
    }

    @After
    public void tearDown() {
    }

    /**
     * Test of learn method, of class RBFNetwork.
     */
    @Test
    public void testLearn() {
        System.out.println("learn");
        ArffParser arffParser = new ArffParser();
        arffParser.setResponseIndex(4);
        try {
            AttributeDataset iris = arffParser.parse(smile.util.Paths.getTestData("weka/iris.arff"));
            double[][] x = iris.toArray(new double[iris.size()][]);
            int[] y = iris.toArray(new int[iris.size()]);

            int n = x.length;
            LOOCV loocv = new LOOCV(n);
            int error = 0;
            for (int i = 0; i < n; i++) {
                double[][] trainx = MathEx.slice(x, loocv.train[i]);
                int[] trainy = MathEx.slice(y, loocv.train[i]);

                double[][] centers = new double[10][];
                RadialBasisFunction[] basis = SmileUtils.learnGaussianRadialBasis(trainx, centers, 5.0);
                RBFNetwork<double[]> rbf = new RBFNetwork<>(trainx, trainy, new EuclideanDistance(), basis, centers);

                if (y[loocv.test[i]] != rbf.predict(x[loocv.test[i]]))
                    error++;
            }

            System.out.println("RBF network error = " + error);
            assertTrue(error <= 6);
        } catch (Exception ex) {
            System.err.println(ex);
        }
    }

    /**
     * Test of learn method, of class RBFNetwork.
     */
    @Test
    public void testSegment() {
        System.out.println("Segment");
        ArffParser parser = new ArffParser();
        parser.setResponseIndex(19);
        try {
            AttributeDataset train = parser.parse(smile.util.Paths.getTestData("weka/segment-challenge.arff"));
            AttributeDataset test = parser.parse(smile.util.Paths.getTestData("weka/segment-test.arff"));

            double[][] x = train.toArray(new double[0][]);
            int[] y = train.toArray(new int[0]);
            double[][] testx = test.toArray(new double[0][]);
            int[] testy = test.toArray(new int[0]);
            
            double[][] centers = new double[100][];
            RadialBasisFunction[] basis = SmileUtils.learnGaussianRadialBasis(x, centers, 5.0);
            RBFNetwork<double[]> rbf = new RBFNetwork<>(x, y, new EuclideanDistance(), basis, centers);
            
            int error = 0;
            for (int i = 0; i < testx.length; i++) {
                if (rbf.predict(testx[i]) != testy[i]) {
                    error++;
                }
            }

            System.out.format("Segment error rate = %.2f%%%n", 100.0 * error / testx.length);
            assertTrue(error <= 210);
        } catch (Exception ex) {
            System.err.println(ex);
        }
    }

    /**
     * Test of learn method, of class RBFNetwork.
     */
    @Test
    public void testUSPS() {
        System.out.println("USPS");
        DelimitedTextParser parser = new DelimitedTextParser();
        parser.setResponseIndex(new NominalAttribute("class"), 0);
        try {
            AttributeDataset train = parser.parse("USPS Train", smile.util.Paths.getTestData("usps/zip.train"));
            AttributeDataset test = parser.parse("USPS Test", smile.util.Paths.getTestData("usps/zip.test"));

            double[][] x = train.toArray(new double[train.size()][]);
            int[] y = train.toArray(new int[train.size()]);
            double[][] testx = test.toArray(new double[test.size()][]);
            int[] testy = test.toArray(new int[test.size()]);
            
            double[][] centers = new double[200][];
            RadialBasisFunction basis = SmileUtils.learnGaussianRadialBasis(x, centers);
            RBFNetwork<double[]> rbf = new RBFNetwork<>(x, y, new EuclideanDistance(), new GaussianRadialBasis(8.0), centers);
                
            int error = 0;
            for (int i = 0; i < testx.length; i++) {
                if (rbf.predict(testx[i]) != testy[i]) {
                    error++;
                }
            }

            System.out.format("USPS error rate = %.2f%%%n", 100.0 * error / testx.length);
            assertTrue(error <= 150);
        } catch (Exception ex) {
            System.err.println(ex);
        }
    }
}
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

import smile.validation.LOOCV;
import smile.data.AttributeDataset;
import smile.data.parser.ArffParser;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import smile.data.NominalAttribute;
import smile.data.parser.DelimitedTextParser;
import smile.math.MathEx;
import smile.sort.QuickSort;

/**
 *
 * @author Haifeng Li
 */
public class GradientTreeBoostTest {
    
    public GradientTreeBoostTest() {
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
     * Test of predict method, of class GradientTreeBoost.
     */
    @Test
    public void testIris2() {
        System.out.println("Iris binary");
        ArffParser arffParser = new ArffParser();
        arffParser.setResponseIndex(4);
        try {
            AttributeDataset iris = arffParser.parse(smile.util.Paths.getTestData("weka/iris.arff"));
            double[][] x = iris.toArray(new double[iris.size()][]);
            int[] y = iris.toArray(new int[iris.size()]);

            for (int i = 0; i < y.length; i++) {
                if (y[i] == 2) {
                    y[i] = 1;
                } else {
                    y[i] = 0;
                }
            }

            int n = x.length;
            LOOCV loocv = new LOOCV(n);
            int error = 0;
            for (int i = 0; i < n; i++) {
                double[][] trainx = MathEx.slice(x, loocv.train[i]);
                int[] trainy = MathEx.slice(y, loocv.train[i]);
                GradientTreeBoost boost = new GradientTreeBoost(iris.attributes(), trainx, trainy, 100);

                if (y[loocv.test[i]] != boost.predict(x[loocv.test[i]]))
                    error++;
            }

            System.out.println("Gradient Tree Boost error = " + error);
            //assertEquals(6, error);
        } catch (Exception ex) {
            System.err.println(ex);
        }
    }
    /**
     * Test of learn method, of class GradientTreeBoost.
     */
    @Test
    public void testIris() {
        System.out.println("Iris");
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
                GradientTreeBoost boost = new GradientTreeBoost(iris.attributes(), trainx, trainy, 100);

                if (y[loocv.test[i]] != boost.predict(x[loocv.test[i]]))
                    error++;
            }

            System.out.println("Gradient Tree Boost error = " + error);
            //assertEquals(6, error);
        } catch (Exception ex) {
            System.err.println(ex);
        }
    }

    /**
     * Test of learn method, of class GradientTreeBoost.
     */
    @Test
    public void testSegment() {
        System.out.println("Segment");
        ArffParser arffParser = new ArffParser();
        arffParser.setResponseIndex(19);
        try {
            AttributeDataset train = arffParser.parse(smile.util.Paths.getTestData("weka/segment-challenge.arff"));
            AttributeDataset test = arffParser.parse(smile.util.Paths.getTestData("weka/segment-test.arff"));

            double[][] x = train.toArray(new double[train.size()][]);
            int[] y = train.toArray(new int[train.size()]);
            double[][] testx = test.toArray(new double[test.size()][]);
            int[] testy = test.toArray(new int[test.size()]);

            GradientTreeBoost boost = new GradientTreeBoost(train.attributes(), x, y, 100);
            
            int error = 0;
            for (int i = 0; i < testx.length; i++) {
                if (boost.predict(testx[i]) != testy[i]) {
                    error++;
                }
            }

            System.out.format("Gradient Tree Boost error rate = %.2f%%%n", 100.0 * error / testx.length);
            //assertEquals(28, error);
        } catch (Exception ex) {
            System.err.println(ex);
        }
    }

    /**
     * Test of learn method, of class GradientTreeBoost.
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
            
            GradientTreeBoost boost = new GradientTreeBoost(train.attributes(), x, y, 100);
            
            int error = 0;
            for (int i = 0; i < testx.length; i++) {
                if (boost.predict(testx[i]) != testy[i]) {
                    error++;
                }
            }

            System.out.format("Gradient Tree Boost error rate = %.2f%%%n", 100.0 * error / testx.length);

            double[] accuracy = boost.test(testx, testy);
            for (int i = 1; i <= accuracy.length; i++) {
                System.out.format("%d trees accuracy = %.2f%%%n", i, 100.0 * accuracy[i-1]);
            }
            
            double[] importance = boost.importance();
            int[] index = QuickSort.sort(importance);
            for (int i = importance.length; i-- > 0; ) {
                System.out.format("%s importance is %.4f%n", train.attributes()[index[i]], importance[i]);
            }
        } catch (Exception ex) {
            System.err.println(ex);
        }
    }

    /**
     * Test of learn method, of class GradientTreeBoost.
     */
    @Test
    public void testUSPS2() {
        System.out.println("USPS 2 classes");
        DelimitedTextParser parser = new DelimitedTextParser();
        parser.setResponseIndex(new NominalAttribute("class"), 0);
        try {
            AttributeDataset train = parser.parse("USPS Train", smile.util.Paths.getTestData("usps/zip.train"));
            AttributeDataset test = parser.parse("USPS Test", smile.util.Paths.getTestData("usps/zip.test"));

            double[][] x = train.toArray(new double[train.size()][]);
            int[] y = train.toArray(new int[train.size()]);
            double[][] testx = test.toArray(new double[test.size()][]);
            int[] testy = test.toArray(new int[test.size()]);
            
            for (int i = 0; i < y.length; i++) {
                if (y[i] != 0) {
                    y[i] = 1;
                }
            }
            for (int i = 0; i < testy.length; i++) {
                if (testy[i] != 0) {
                    testy[i] = 1;
                }
            }
            
            GradientTreeBoost boost = new GradientTreeBoost(train.attributes(), x, y, 100);
            
            int error = 0;
            for (int i = 0; i < testx.length; i++) {
                if (boost.predict(testx[i]) != testy[i]) {
                    error++;
                }
            }

            System.out.format("Gradient Tree Boost error rate = %.2f%%%n", 100.0 * error / testx.length);
            
            double[] accuracy = boost.test(testx, testy);
            for (int i = 1; i <= accuracy.length; i++) {
                System.out.format("%d trees accuracy = %.2f%%%n", i, 100.0 * accuracy[i-1]);
            }
            
            double[] importance = boost.importance();
            int[] index = QuickSort.sort(importance);
            for (int i = importance.length; i-- > 0; ) {
                System.out.format("%s importance is %.4f%n", train.attributes()[index[i]], importance[i]);
            }
        } catch (Exception ex) {
            System.err.println(ex);
        }
    }
}

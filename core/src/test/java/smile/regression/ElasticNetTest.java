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

package smile.regression;

import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import smile.data.*;
import smile.data.formula.Formula;
import smile.data.vector.DoubleVector;
import smile.math.MathEx;
import smile.validation.CrossValidation;
import smile.validation.LOOCV;
import smile.validation.Validation;

import static org.junit.Assert.assertEquals;

/**
 *
 * @author rayeaster
 */
public class ElasticNetTest {
    public ElasticNetTest() {
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
    @Test
    public void testToy() {
        double[][] A = {
                {1, 0, 0, 0.5},
                {0, 1, 0.2, 0.3},
                {1, 0.5, 0.2, 0.3},
                {0, 0.1, 0, 0.2},
                {0, 0.1, 1, 0.2}
        };

        double[] y = {6, 5.2, 6.2, 5, 6};

        DataFrame df = DataFrame.of(A).merge(DoubleVector.of("y", y));

        LinearModel model = ElasticNet.fit(Formula.lhs("y"), df, 0.8, 0.2);
        System.out.println(model);

        double rmse = Validation.test(model, df);
        System.out.println("RMSE = " + rmse);

        assertEquals(5.0259443688265355, model.intercept(), 1E-7);
        double[] w = {0.9659945126777854, -3.7147706312985876E-4, 0.9553629503697613, 9.416740009376934E-4};
        for (int i = 0; i < w.length; i++) {
            assertEquals(w[i], model.coefficients()[i], 1E-5);
        }
    }

    @Test
    public void testLongley() {
        System.out.println("longley");

        LinearModel model = ElasticNet.fit(Longley.formula, Longley.data, 0.8, 0.2);
        System.out.println(model);

        double rmse = LOOCV.test(Longley.data, (x) -> ElasticNet.fit(Longley.formula, x, 0.8, 0.2));
        System.out.println("LOOCV RMSE = " + rmse);
        assertEquals(1.4146564289679233, rmse, 1E-4);
    }


    /**
     * Test of learn method, of class LinearRegression.
     */
    @Test
    public void testCPU() {
        System.out.println("CPU");
        LinearModel model = ElasticNet.fit(CPU.formula, CPU.data, 0.8, 0.2);
        System.out.println(model);

        double rmse = CrossValidation.test(10, CPU.data, (x) -> ElasticNet.fit(CPU.formula, x, 0.8, 0.2));
        System.out.println("10-CV RMSE = " + rmse);
        assertEquals(1.4146564289679233, rmse, 1E-4);
    }

    /**
     * Test of learn method, of class LinearRegression.
     */
    @Test
    public void tesProstate() {
        System.out.println("Prostate");
        LinearModel model = ElasticNet.fit(Prostate.formula, Prostate.train, 0.8, 0.2);
        System.out.println(model);

        double rmse = Validation.test(model, Prostate.test);
        System.out.println("Test RMSE = " + rmse);
        assertEquals(1.4146564289679233, rmse, 1E-4);
    }

    /**
     * Test of learn method, of class LinearRegression.
     */
    @Test
    public void tesAbalone() {
        System.out.println("Abalone");
        LinearModel model = ElasticNet.fit(Abalone.formula, Abalone.train, 0.8, 0.2);
        System.out.println(model);

        double rmse = Validation.test(model, Abalone.test);
        System.out.println("Test RMSE = " + rmse);
        assertEquals(1.4146564289679233, rmse, 1E-4);
    }

    /**
     * Test of learn method, of class LinearRegression.
     */
    @Test
    public void tesDiabetes() {
        System.out.println("Diabetes");
        LinearModel model = ElasticNet.fit(Diabetes.formula, Diabetes.data, 0.8, 0.2);
        System.out.println(model);

        double rmse = CrossValidation.test(10, Diabetes.data, (x) -> ElasticNet.fit(Diabetes.formula, x, 0.8, 0.2));
        System.out.println("Diabetes 10-CV RMSE = " + rmse);
        assertEquals(1.4146564289679233, rmse, 1E-4);
    }
}
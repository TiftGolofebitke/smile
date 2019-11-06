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

package smile.clustering;

import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import static org.junit.Assert.*;

import smile.data.Dataset;
import smile.data.Instance;
import smile.io.DatasetReader;
import smile.math.MathEx;
import smile.util.SparseArray;
import smile.validation.AdjustedRandIndex;
import smile.validation.RandIndex;

/**
 *
 * @author Haifeng
 */
public class SIBTest {
    
    public SIBTest() {
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

    @Test(expected = Test.None.class)
    public void testParseNG20() throws Exception {
        System.out.println("NG20");

        MathEx.setSeed(19650218); // to get repeatable results.

        Dataset<Instance<SparseArray>> train = DatasetReader.libsvm(smile.util.Paths.getTestData("libsvm/news20.dat"));
        Dataset<Instance<SparseArray>> test = DatasetReader.libsvm(smile.util.Paths.getTestData("libsvm/news20.t.dat"));

        SparseArray[] trainx = train.stream().map(Instance<SparseArray>::x).toArray(SparseArray[]::new);
        int[] y = train.stream().mapToInt(i -> i.label()).toArray();
        int[] testy = test.stream().mapToInt(i -> i.label()).toArray();
            
        SIB sib = SIB.fit(trainx, 20);
        System.out.println(sib);

        double r = RandIndex.of(y, sib.y);
        double r2 = AdjustedRandIndex.of(y, sib.y);
        System.out.format("Training rand index = %.2f%%, adjusted rand index = %.2f%%%n", 100.0 * r, 100.0 * r2);
        assertEquals(0.8842, r, 1E-4);
        assertEquals(0.2327, r2, 1E-4);

        int[] p = new int[test.size()];
        for (int i = 0; i < test.size(); i++) {
            p[i] = sib.predict(test.get(i).x());
        }
            
        r = RandIndex.of(testy, p);
        r2 = AdjustedRandIndex.of(testy, p);
        System.out.format("Testing rand index = %.2f%%, adjusted rand index = %.2f%%%n", 100.0 * r, 100.0 * r2);
        assertEquals(0.8782, r, 1E-4);
        assertEquals(0.2287, r2, 1E-4);
    }
}

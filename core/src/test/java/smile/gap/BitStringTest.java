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

package smile.gap;

import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import static org.junit.Assert.*;

/**
 *
 * @author Haifeng Li
 */
public class BitStringTest {

    public BitStringTest() {
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
     * Test of newInstance method, of class BitString.
     */
    @Test
    public void testNewInstance() {
        System.out.println("newInstance");
        int[] father = {1,1,1,0,1,0,0,1,0,0,0};
        BitString instance = new BitString(father, null, BitString.Crossover.SINGLE_POINT, 1.0, 0.0);
        BitString result = instance.newInstance();
        assertEquals(father.length, result.length);
        assertEquals(father.length, result.bits().length);
        boolean same = true;
        for (int i = 0; i < father.length; i++) {
            if (father[i] != result.bits()[i]) {
                same = false;
            }
        }
        assertFalse(same);
    }

    /**
     * Test of crossover method, of class BitString.
     */
    @Test
    public void testCrossoverOne() {
        System.out.println("crossover one point");
        int[] father = {1,1,1,0,1,0,0,1,0,0,0};
        int[] mother = {0,0,0,0,1,0,1,0,1,0,1};
        BitString instance = new BitString(father, null, BitString.Crossover.SINGLE_POINT, 1.0, 0.0);
        BitString another = new BitString(mother, null, BitString.Crossover.SINGLE_POINT, 1.0, 0.0);
        int[] son = {1,1,1,0,1,0,1,0,1,0,1};
        int[] daughter = {0,0,0,0,1,0,0,1,0,0,0};
        BitString[] result = instance.crossover(another);
        assertEquals(son.length, result[0].bits().length);
        assertEquals(daughter.length, result[1].bits().length);
        for (int i = 0; i< son.length; i++) {
            //assertEquals(son[i], result[0].bits()[i]);
            //assertEquals(daughter[i], result[1].bits()[i]);
            assertTrue((father[i] == result[0].bits()[i] && mother[i] == result[1].bits()[i])
                    || (father[i] == result[1].bits()[i] && mother[i] == result[0].bits()[i]));
        }
    }

    /**
     * Test of crossover method, of class BitString.
     */
    @Test
    public void testCrossoverTwo() {
        System.out.println("crossover two point");
        int[] father = {1,1,1,0,1,0,0,1,0,0,0};
        int[] mother = {0,0,0,0,1,0,1,0,1,0,1};
        BitString instance = new BitString(father, null, BitString.Crossover.TWO_POINT, 1.0, 0.0);
        BitString another = new BitString(mother, null, BitString.Crossover.TWO_POINT, 1.0, 0.0);
        int[] son = {1,1,0,0,1,0,1,1,0,0,0};
        int[] daughter = {0,0,1,0,1,0,0,0,1,0,1};
        BitString[] result = instance.crossover(another);
        assertEquals(son.length, result[0].bits().length);
        assertEquals(daughter.length, result[1].bits().length);
        for (int i = 0; i< son.length; i++) {
            //assertEquals(son[i], result[0].bits()[i]);
            //assertEquals(daughter[i], result[1].bits()[i]);
            assertTrue((father[i] == result[0].bits()[i] && mother[i] == result[1].bits()[i])
                    || (father[i] == result[1].bits()[i] && mother[i] == result[0].bits()[i]));
        }
    }

    /**
     * Test of crossover method, of class BitString.
     */
    @Test
    public void testCrossoverUniform() {
        System.out.println("crossover uniform");
        int[] father = {1,1,1,0,1,0,0,1,0,0,0};
        int[] mother = {0,0,0,0,1,0,1,0,1,0,1};
        BitString instance = new BitString(father, null, BitString.Crossover.UNIFORM, 1.0, 0.0);
        BitString another = new BitString(mother, null, BitString.Crossover.UNIFORM, 1.0, 0.0);
        BitString[] result = instance.crossover(another);
        assertEquals(father.length, result[0].bits().length);
        assertEquals(mother.length, result[1].bits().length);

        boolean same = true;
        for (int i = 0; i< father.length; i++) {
            assertTrue((father[i] == result[0].bits()[i] && mother[i] == result[1].bits()[i])
                    || (father[i] == result[1].bits()[i] && mother[i] == result[0].bits()[i]));

            if (father[i] != result[0].bits()[i]) {
                same = false;
            }
        }

        assertFalse(same);
    }

    /**
     * Test of mutate method, of class BitString.
     */
    @Test
    public void testMutate() {
        System.out.println("mutate");
        int[] father = {1,1,1,0,1,0,0,1,0,0,0};
        BitString instance = new BitString(father.clone(), null, BitString.Crossover.SINGLE_POINT, 1.0, 1.0);
        instance.mutate();
        assertEquals(father.length, instance.length);
        assertEquals(father.length, instance.bits().length);
        boolean same = true;
        for (int i = 0; i < father.length; i++) {
            if (father[i] != instance.bits()[i]) {
                same = false;
            }
        }
        assertFalse(same);
    }

}
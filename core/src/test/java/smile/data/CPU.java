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

package smile.data;

import smile.data.formula.Formula;
import smile.io.Arff;
import smile.util.Paths;

/**
 *
 * @author Haifeng
 */
public class CPU {

    public static DataFrame data;
    public static Formula formula = Formula.lhs("class");

    public static double[][] x;
    public static double[] y;

    static {
        try {
            Arff arff = new Arff(Paths.getTestData("weka/cpu.arff"));
            data = arff.read();

            x = data.drop("class").toArray();
            y = data.column("class").toDoubleArray();
        } catch (Exception ex) {
            System.err.println("Failed to load 'CPU': " + ex);
            System.exit(-1);
        }
    }
}

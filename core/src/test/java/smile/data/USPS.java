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

import org.apache.commons.csv.CSVFormat;
import smile.data.formula.Formula;
import smile.data.type.DataTypes;
import smile.data.type.StructField;
import smile.data.type.StructType;
import smile.io.CSV;
import smile.util.Paths;

import java.util.ArrayList;
import java.util.stream.IntStream;

/**
 *
 * @author Haifeng
 */
public class USPS {

    public static DataFrame train;
    public static DataFrame test;
    public static Formula formula = Formula.lhs("class");

    public static double[][] x;
    public static int[] y;
    public static double[][] testx;
    public static int[] testy;

    static {
        ArrayList<StructField> fields = new ArrayList<>();
        fields.add(new StructField("class", DataTypes.ByteType));
        IntStream.range(0, 256).forEach(i -> fields.add(new StructField("V"+i, DataTypes.DoubleType)));
        StructType schema = DataTypes.struct(fields);

        CSV csv = new CSV(CSVFormat.DEFAULT.withDelimiter(' '));
        csv.schema(schema);

        try {
            train = csv.read(Paths.getTestData("usps/zip.train"));
            test = csv.read(Paths.getTestData("usps/zip.test"));

            x = train.drop("class").toArray();
            y = train.column("class").toIntArray();
            testx = test.drop("class").toArray();
            testy = test.column("class").toIntArray();
        } catch (Exception ex) {
            System.err.println("Failed to load 'USPS': " + ex);
            System.exit(-1);
        }
    }
}

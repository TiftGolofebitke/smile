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

package smile.data.formula;

import smile.data.Tuple;
import smile.data.type.DataType;
import smile.data.vector.*;
import smile.data.DataFrame;
import java.util.Collections;
import java.util.List;

/**
 * A term is recursively constructed from constant symbols,
 * variables and function symbols. A term returns a single value
 * when applied to a data object (e.g. Tuple).
 *
 * @author Haifeng Li
 */
public interface Term extends HyperTerm {
    @Override
    default List<Term> terms() {
        return Collections.singletonList(this);
    }

    /** Returns the data type of output values. */
    DataType type();

    /** Applies the term on a data object. */
    Object apply(Tuple o);

    /** Applies the term on a data object and produces an double-valued result. */
    default double applyAsDouble(Tuple o) {
        throw new UnsupportedOperationException();
    }

    /** Applies the term on a data object and produces an float-valued result. */
    default float applyAsFloat(Tuple o) {
        throw new UnsupportedOperationException();
    }

    /** Applies the term on a data object and produces an int-valued result. */
    default int applyAsInt(Tuple o) {
        throw new UnsupportedOperationException();
    }

    /** Applies the term on a data object and produces an long-valued result. */
    default long applyAsLong(Tuple o) {
        throw new UnsupportedOperationException();
    }

    /** Applies the term on a data object and produces an boolean-valued result. */
    default boolean applyAsBoolean(Tuple o) {
        throw new UnsupportedOperationException();
    }

    /** Applies the term on a data object and produces an byte-valued result. */
    default byte applyAsByte(Tuple o) {
        throw new UnsupportedOperationException();
    }

    /** Applies the term on a data object and produces an short-valued result. */
    default short applyAsShort(Tuple o) {
        throw new UnsupportedOperationException();
    }

    /** Applies the term on a data object and produces an char-valued result. */
    default char applyAsChar(Tuple o) {
        throw new UnsupportedOperationException();
    }

    /** Returns true if the term represents a plain variable. */
    default boolean isVariable() {
        return false;
    }

    /** Returns true if the term represents a constant value. */
    default boolean isConstant() {
        return false;
    }

    default BaseVector apply(DataFrame df) {
        if (isVariable()) {
            return df.column(toString());
        }

        int size = df.size();
        switch (type().id()) {
            case Integer: {
                int[] values = new int[size];
                for (int i = 0; i < size; i++) values[i] = applyAsInt(df.get(i));
                return IntVector.of(toString(), values);
            }

            case Long: {
                long[] values = new long[size];
                for (int i = 0; i < size; i++) values[i] = applyAsLong(df.get(i));
                return LongVector.of(toString(), values);
            }

            case Double: {
                double[] values = new double[size];
                for (int i = 0; i < size; i++) values[i] = applyAsDouble(df.get(i));
                return DoubleVector.of(toString(), values);
            }

            case Float: {
                float[] values = new float[size];
                for (int i = 0; i < size; i++) values[i] = applyAsFloat(df.get(i));
                return FloatVector.of(toString(), values);
            }

            case Boolean: {
                boolean[] values = new boolean[size];
                for (int i = 0; i < size; i++) values[i] = applyAsBoolean(df.get(i));
                return BooleanVector.of(toString(), values);
            }

            case Byte: {
                byte[] values = new byte[size];
                for (int i = 0; i < size; i++) values[i] = applyAsByte(df.get(i));
                return ByteVector.of(toString(), values);
            }

            case Short: {
                short[] values = new short[size];
                for (int i = 0; i < size; i++) values[i] = applyAsShort(df.get(i));
                return ShortVector.of(toString(), values);
            }

            case Char: {
                char[] values = new char[size];
                for (int i = 0; i < size; i++) values[i] = applyAsChar(df.get(i));
                return CharVector.of(toString(), values);
            }

            default: {
                Object[] values = new Object[size];
                for (int i = 0; i < size; i++) values[i] = apply(df.get(i));
                return Vector.of(toString(), type(), values);
            }
        }
    }
}

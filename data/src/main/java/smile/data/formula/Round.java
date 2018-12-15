/*******************************************************************************
 * Copyright (c) 2010 Haifeng Li
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *******************************************************************************/
package smile.data.formula;

import java.util.Collections;
import java.util.List;
import java.util.Set;

import smile.data.Tuple;
import smile.data.type.DataType;
import smile.data.type.DataTypes;
import smile.data.type.StructType;

/**
 * The term of round function.
 *
 * @author Haifeng Li
 */
class Round implements Function {
    /** The operand factor of round expression. */
    private Function child;

    /**
     * Constructor.
     *
     * @param factor the factor that round function is applied to.
     */
    public Round(Function factor) {
        this.child = factor;
    }

    @Override
    public String name() {
        return String.format("round(%s)", child.name());
    }

    @Override
    public String toString() {
        return name();
    }

    @Override
    public boolean equals(Object o) {
        return name().equals(o);
    }

    @Override
    public List<? extends Function> factors() {
        return Collections.singletonList(this);
    }

    @Override
    public Set<String> variables() {
        return child.variables();
    }

    @Override
    public DataType type() {
        if (child.type().equals(DataTypes.DoubleType)) return DataTypes.LongType;
        if (child.type().equals(DataTypes.FloatType)) return DataTypes.IntegerType;
        if (child.type().equals(DataTypes.object(Double.class))) return DataTypes.object(Long.class);
        else return DataTypes.object(Integer.class);
    }

    @Override
    public void bind(StructType schema) {
        child.bind(schema);

        if (!(child.type().isDouble() || child.type().isFloat())) {
            throw new IllegalStateException(String.format("Invalid expression: round(%s)", child.type()));
        }
    }

    @Override
    public double applyAsDouble(Tuple o) {
        return Math.round(child.applyAsDouble(o));
    }

    @Override
    public Long apply(Tuple o) {
        Object x = child.apply(o);
        if (x == null) return null;
        else return Math.round(((Number) x).doubleValue());
    }
}

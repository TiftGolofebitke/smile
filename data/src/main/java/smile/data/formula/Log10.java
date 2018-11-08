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

/**
 * The term of log10 function.
 *
 * @author Haifeng Li
 */
public class Log10<T> implements Factor<T, Double> {
    /** The operand factor of log10 expression. */
    private Factor<T, Double> child;

    /**
     * Constructor.
     *
     * @param factor the factor that log10 function is applied to.
     */
    public Log10(Factor<T, Double> factor) {
        this.child = factor;
    }

    /**
     * Constructor.
     *
     * @param column the variable that log10 function is applied to.
     */
    public Log10(String column) {
        this.child = new Column(column);
    }

    @Override
    public String name() {
        return String.format("log10(%s)", child.name());
    }

    @Override
    public List<Factor> factors() {
        return Collections.singletonList(this);
    }

    @Override
    public Set<String> variables() {
        return child.variables();
    }

    @Override
    public Double apply(T o) {
        return Math.log10(child.apply(o));
    }
}

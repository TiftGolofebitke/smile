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
 * The term of abs function.
 *
 * @author Haifeng Li
 */
public class Abs implements Factor {
    /** The operand factor of abs expression. */
    private Factor child;

    /**
     * Constructor.
     *
     * @param factor the factor that abs function is applied to.
     */
    public Abs(Factor factor) {
        this.child = factor;
    }

    /**
     * Constructor.
     *
     * @param token the variable that abs function is applied to.
     */
    public Abs(String token) {
        this.child = new Token(token);
    }

    @Override
    public String name() {
        return String.format("abs(%s)", child.name());
    }

    @Override
    public List<Factor> factors() {
        return Collections.singletonList(this);
    }

    @Override
    public Set<String> tokens() {
        return child.tokens();
    }

    @Override
    public double apply(Tuple tuple) {
        return Math.abs(child.apply(tuple));
    }
}

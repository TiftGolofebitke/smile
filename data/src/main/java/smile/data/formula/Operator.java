/*
 * Copyright (c) 2010-2021 Haifeng Li. All rights reserved.
 *
 * Smile is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Smile is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Smile.  If not, see <https://www.gnu.org/licenses/>.
 */

package smile.data.formula;

/**
 * The infix bifunction term.
 *
 * @author Haifeng Li
 */
public abstract class Operator extends AbstractBiFunction {
    /**
     * Constructor.
     *
     * @param name the operator name.
     * @param x the left operand.
     * @param y the right operand.
     */
    public Operator(String name, Term x, Term y) {
        super(name, x, y);
    }

    @Override
    public String toString() {
        return String.format("(%s %s %s)", x, name, y);
    }
}

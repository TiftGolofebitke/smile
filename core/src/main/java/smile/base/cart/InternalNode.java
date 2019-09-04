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

package smile.base.cart;

import smile.data.Tuple;
import smile.regression.Regression;

/**
 * An internal node in CART.
 */
public abstract class InternalNode implements Node {

    /**
     * Children node.
     */
    Node trueChild;

    /**
     * Children node.
     */
    Node falseChild;

    /**
     * The split feature for this node.
     */
    int splitFeature = -1;

    /**
     * Reduction in impurity compared to parent.
     */
    double splitScore = 0.0;

    public InternalNode(int splitFeature, double splitScore, Node trueChild, Node falseChild) {
        this.splitFeature = splitFeature;
        this.splitScore = splitScore;
        this.trueChild = trueChild;
        this.falseChild = falseChild;
    }

    /**
     * Evaluate the tree over an instance.
     */
    public abstract LeafNode predict(Tuple x);

    @Override
    public int depth() {
        // compute the depth of each subtree
        int ld = trueChild.depth();
        int rd = falseChild.depth();

        // use the larger one
        return Math.max(ld, rd) + 1;
    }

    @Override
    public Node toLeaf() {
        trueChild = trueChild.toLeaf();
        falseChild = falseChild.toLeaf();

        if (trueChild instanceof DecisionNode && falseChild instanceof DecisionNode) {
            if (((DecisionNode) trueChild).output() == ((DecisionNode) falseChild).output()) {
                return new DecisionNode(((DecisionNode) trueChild).output());
            }

        } else if (trueChild instanceof RegressionNode && falseChild instanceof RegressionNode) {
            if (((RegressionNode) trueChild).output() == ((RegressionNode) falseChild).output()) {
                return new RegressionNode(((RegressionNode) trueChild).output());
            }
        }

        return this;
    }
}

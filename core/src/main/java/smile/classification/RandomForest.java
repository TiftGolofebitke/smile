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

package smile.classification;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import smile.base.cart.CART;
import smile.base.cart.SplitRule;
import smile.data.DataFrame;
import smile.data.Tuple;
import smile.data.formula.Formula;
import smile.data.vector.BaseVector;
import smile.math.MathEx;
import smile.validation.Accuracy;
import smile.validation.ClassificationMeasure;

/**
 * Random forest for classification. Random forest is an ensemble classifier
 * that consists of many decision trees and outputs the majority vote of
 * individual trees. The method combines bagging idea and the random
 * selection of features.
 * <p>
 * Each tree is constructed using the following algorithm:
 * <ol>
 * <li> If the number of cases in the training set is N, randomly sample N cases
 * with replacement from the original data. This sample will
 * be the training set for growing the tree. 
 * <li> If there are M input variables, a number m &lt;&lt; M is specified such
 * that at each node, m variables are selected at random out of the M and
 * the best split on these m is used to split the node. The value of m is
 * held constant during the forest growing. 
 * <li> Each tree is grown to the largest extent possible. There is no pruning. 
 * </ol>
 * The advantages of random forest are:
 * <ul>
 * <li> For many data sets, it produces a highly accurate classifier.
 * <li> It runs efficiently on large data sets.
 * <li> It can handle thousands of input variables without variable deletion.
 * <li> It gives estimates of what variables are important in the classification.
 * <li> It generates an internal unbiased estimate of the generalization error
 * as the forest building progresses.
 * <li> It has an effective method for estimating missing data and maintains
 * accuracy when a large proportion of the data are missing.
 * </ul>
 * The disadvantages are
 * <ul>
 * <li> Random forests are prone to over-fitting for some datasets. This is
 * even more pronounced on noisy data.
 * <li> For data including categorical variables with different number of
 * levels, random forests are biased in favor of those attributes with more
 * levels. Therefore, the variable importance scores from random forest are
 * not reliable for this type of data.
 * </ul>
 * 
 * @author Haifeng Li
 */
public class RandomForest implements SoftClassifier<Tuple> {
    private static final long serialVersionUID = 2L;
    private static final org.slf4j.Logger logger = org.slf4j.LoggerFactory.getLogger(RandomForest.class);

    /**
     * Decision tree wrapper with a weight. Currently, the weight is the accuracy of
     * tree on the OOB samples, which can be used when aggregating
     * tree votes.
     */
    static class Tree implements Serializable {
        DecisionTree tree;
        double weight;
        Tree(DecisionTree tree, double weight) {
            this.tree = tree;
            this.weight = weight;
        }
    }

    /**
     * Design matrix formula
     */
    private Formula formula;

    /**
     * Forest of decision trees. The second value is the accuracy of
     * tree on the OOB samples, which can be used a weight when aggregating
     * tree votes.
     */
    private List<Tree> trees;
    /**
     * The number of classes.
     */
    private int k = 2;
    /**
     * Out-of-bag estimation of error rate, which is quite accurate given that
     * enough trees have been grown (otherwise the OOB estimate can
     * bias upward).
     */
    private double error;
    /**
     * Variable importance. Every time a split of a node is made on variable
     * the (GINI, information gain, etc.) impurity criterion for the two
     * descendent nodes is less than the parent node. Adding up the decreases
     * for each individual variable over all trees in the forest gives a fast
     * variable importance that is often very consistent with the permutation
     * importance measure.
     */
    private double[] importance;

    /**
     * Constructor.
     */
    public RandomForest(Formula formula, List<Tree> trees, double error, double[] importance) {
        this.formula = formula;
        this.trees = trees;
        this.error = error;
        this.importance = importance;
    }

    /**
     * Learns a random forest for classification.
     *
     * @param formula a symbolic description of the model to be fitted.
     * @param data the data frame of the explanatory and response variables.
     * @param ntrees the number of trees.
     * @param mtry the number of random selected features to be used to determine
     * the decision at a node of the tree. floor(sqrt(dim)) seems to give
     * generally good performance, where dim is the number of variables.
     * @param nodeSize the minimum size of leaf nodes.
     * @param maxNodes the maximum number of leaf nodes in the tree.
     * @param subsample the sampling rate for training tree. 1.0 means sampling with replacement. < 1.0 means
     *                  sampling without replacement.
     * @param rule Decision tree split rule.
     * @param classWeight Priors of the classes. The weight of each class
     *                    is roughly the ratio of samples in each class.
     *                    For example, if
     *                    there are 400 positive samples and 100 negative
     *                    samples, the classWeight should be [1, 4]
     *                    (assuming label 0 is of negative, label 1 is of
     *                    positive).
     */
    public static RandomForest fit(Formula formula, DataFrame data, int ntrees, int maxNodes, int nodeSize, int mtry, double subsample, SplitRule rule, int[] classWeight) {
        if (ntrees < 1) {
            throw new IllegalArgumentException("Invalid number of trees: " + ntrees);
        }

        if (nodeSize < 1) {
            throw new IllegalArgumentException("Invalid minimum size of leaves: " + nodeSize);
        }

        if (maxNodes < 2) {
            throw new IllegalArgumentException("Invalid maximum number of leaves: " + maxNodes);
        }

        if (subsample <= 0 || subsample > 1) {
            throw new IllegalArgumentException("Invalid sampling rating: " + subsample);
        }

        DataFrame x = formula.frame(data);
        BaseVector y = formula.response(data);

        if (mtry < 1 || mtry > x.ncols()) {
            throw new IllegalArgumentException("Invalid number of variables to split on at a node of the tree: " + mtry);
        }

        final int n = x.nrows();
        final int k = Classifier.classes(y).length;

        final int[] weight = classWeight != null ? classWeight : Collections.nCopies(k, 1).stream().mapToInt(i -> i).toArray();

        final int[][] order = CART.order(x);
        final int[][] prediction = new int[n][k]; // out-of-bag prediction

        List<Tree> trees = IntStream.range(0, ntrees).parallel().mapToObj(t -> {
            int[] samples = new int[n];

            // Stratified sampling in case class is unbalanced.
            // That is, we sample each class separately.
            if (subsample == 1.0) {
                // Training samples draw with replacement.
                IntStream.range(0, k).forEach(l -> {
                    int[] cl = IntStream.range(0, n).filter(i -> y.getInt(i) == l).toArray();

                    // We used to do up sampling.
                    // But we switch to down sampling, which seems has better performance.
                    int size = cl.length / weight[l];
                    for (int i = 0; i < size; i++) {
                        int xi = MathEx.randomInt(cl.length);
                        samples[cl[xi]] += 1; //classWeight[l];
                    }
                });
            } else {
                // Training samples draw without replacement.
                IntStream.range(0, k).forEach(l -> {
                    int[] cl = IntStream.range(0, n).filter(i -> y.getInt(i) == l).toArray();

                    // We used to do up sampling.
                    // But we switch to down sampling, which seems has better performance.
                    int size = (int) Math.round(subsample * cl.length / weight[l]);
                    int[] perm = IntStream.range(0, cl.length).toArray();
                    MathEx.permutate(perm);
                    for (int i = 0; i < size; i++) {
                        samples[cl[perm[i]]] += 1; //classWeight[l];
                    }
                });
            }

            DecisionTree tree = new DecisionTree(x, y, k, rule, nodeSize, maxNodes, mtry, samples, order);

            // estimate OOB error
            int oob = 0;
            int correct = 0;
            for (int i = 0; i < n; i++) {
                if (samples[i] == 0) {
                    oob++;
                    int p = tree.predict(x.get(i));
                    if (p == y.getInt(i)) correct++;
                    // atomic operaton. do we really need synchronized?
                    synchronized (prediction[i]) {
                        prediction[i][p]++;
                    }
                }
            }

            double accuracy = 1.0;
            if (oob != 0) {
                accuracy = (double) correct / oob;
                logger.info("Random forest tree OOB size: {}, accuracy: {}", oob, String.format("%.2f%%", 100 * accuracy));
            } else {
                logger.error("Random forest has a tree trained without OOB samples.");
            }

            return new Tree(tree, accuracy);
        }).collect(Collectors.toList());

        int err = 0;
        int m = 0;
        for (int i = 0; i < n; i++) {
            int pred = MathEx.whichMax(prediction[i]);
            if (prediction[i][pred] > 0) {
                m++;
                if (pred != y.getInt(i)) {
                    err++;
                }
            }
        }

        double error = m > 0 ? (double) err / m : 0.0;

        double[] importance = new double[x.ncols()];
        for (Tree tree : trees) {
            double[] imp = tree.tree.importance();
            for (int i = 0; i < imp.length; i++) {
                importance[i] += imp[i];
            }
        }

        return new RandomForest(formula, trees, error, importance);
    }

    /**
     * Returns the out-of-bag estimation of error rate. The OOB estimate is
     * quite accurate given that enough trees have been grown. Otherwise the
     * OOB estimate can bias upward.
     * 
     * @return the out-of-bag estimation of error rate
     */
    public double error() {
        return error;
    }
    
    /**
     * Returns the variable importance. Every time a split of a node is made
     * on variable the (GINI, information gain, etc.) impurity criterion for
     * the two descendent nodes is less than the parent node. Adding up the
     * decreases for each individual variable over all trees in the forest
     * gives a fast measure of variable importance that is often very
     * consistent with the permutation importance measure.
     *
     * @return the variable importance
     */
    public double[] importance() {
        return importance;
    }
    
    /**
     * Returns the number of trees in the model.
     * 
     * @return the number of trees in the model 
     */
    public int size() {
        return trees.size();
    }
    
    /**
     * Trims the tree model set to a smaller size in case of over-fitting.
     * Or if extra decision trees in the model don't improve the performance,
     * we may remove them to reduce the model size and also improve the speed of
     * prediction.
     * 
     * @param ntrees the new (smaller) size of tree model set.
     */
    public void trim(int ntrees) {
        if (ntrees > trees.size()) {
            throw new IllegalArgumentException("The new model size is larger than the current size.");
        }
        
        if (ntrees <= 0) {
            throw new IllegalArgumentException("Invalid new model size: " + ntrees);
        }

        List<Tree> model = new ArrayList<>(ntrees);
        for (int i = 0; i < ntrees; i++) {
            model.add(trees.get(i));
        }
        
        trees = model;
    }
    
    @Override
    public int predict(Tuple x) {
        int[] y = new int[k];
        
        for (Tree tree : trees) {
            y[tree.tree.predict(x)]++;
        }
        
        return MathEx.whichMax(y);
    }
    
    @Override
    public int predict(Tuple x, double[] posteriori) {
        if (posteriori.length != k) {
            throw new IllegalArgumentException(String.format("Invalid posteriori vector size: %d, expected: %d", posteriori.length, k));
        }

        Arrays.fill(posteriori, 0.0);

        int[] y = new int[k];
        double[] pos = new double[k];
        for (Tree tree : trees) {
            y[tree.tree.predict(x, pos)]++;
            for (int i = 0; i < k; i++) {
                posteriori[i] += tree.weight * pos[i];
            }
        }

        MathEx.unitize1(posteriori);
        return MathEx.whichMax(y);
    }    
    
    /**
     * Test the model on a validation dataset.
     *
     * @param data the test data set.
     * @return accuracies with first 1, 2, ..., decision trees.
     */
    public double[] test(DataFrame data) {
        DataFrame x = formula.frame(data);
        int[] y = formula.response(data).toIntArray();

        int T = trees.size();
        double[] accuracy = new double[T];

        int n = x.nrows();
        int[] label = new int[n];
        int[][] prediction = new int[n][k];

        Accuracy measure = new Accuracy();
        
        for (int i = 0; i < T; i++) {
            for (int j = 0; j < n; j++) {
                prediction[j][trees.get(i).tree.predict(x.get(j))]++;
                label[j] = MathEx.whichMax(prediction[j]);
            }

            accuracy[i] = measure.measure(y, label);
        }

        return accuracy;
    }
    
    /**
     * Test the model on a validation dataset.
     * 
     * @param data the test data set.
     * @param measures the performance measures of classification.
     * @return performance measures with first 1, 2, ..., decision trees.
     */
    public double[][] test(DataFrame data, ClassificationMeasure[] measures) {
        DataFrame x = formula.frame(data);
        int[] y = formula.response(data).toIntArray();

        int T = trees.size();
        int m = measures.length;
        double[][] results = new double[T][m];

        int n = x.nrows();
        int[] label = new int[n];
        double[][] prediction = new double[n][k];

        for (int i = 0; i < T; i++) {
            for (int j = 0; j < n; j++) {
                prediction[j][trees.get(i).tree.predict(x.get(j))]++;
                label[j] = MathEx.whichMax(prediction[j]);
            }

            for (int j = 0; j < m; j++) {
                results[i][j] = measures[j].measure(y, label);
            }
        }
        return results;
    }

    /**
     * Returns the decision trees.
     */
    public DecisionTree[] getTrees() {
        return trees.stream().map(t -> t.tree).toArray(DecisionTree[]::new);
    }
}

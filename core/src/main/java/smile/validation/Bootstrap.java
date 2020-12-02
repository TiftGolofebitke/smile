/*******************************************************************************
 * Copyright (c) 2010-2020 Haifeng Li. All rights reserved.
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
 ******************************************************************************/

package smile.validation;

import java.util.function.BiFunction;
import smile.classification.Classifier;
import smile.classification.DataFrameClassifier;
import smile.data.DataFrame;
import smile.data.formula.Formula;
import smile.math.MathEx;
import smile.regression.Regression;
import smile.regression.DataFrameRegression;

/**
 * The bootstrap is a general tool for assessing statistical accuracy. The basic
 * idea is to randomly draw datasets with replacement from the training data,
 * each samples the same size as the original training set. This is done many
 * times (say k = 100), producing k bootstrap datasets. Then we refit the model
 * to each of the bootstrap datasets and examine the behavior of the fits over
 * the k replications.
 *
 * @author Haifeng Li
 */
public interface Bootstrap {
    /**
     * Creates k rounds of bootstrap sampling.
     * @param n the number of samples.
     * @param k the number of rounds of bootstrap.
     * @return k rounds of data splits. The test data are out of bag (OOB) samples.
     */
    static Split[] of(int n, int k) {
        if (n < 0) {
            throw new IllegalArgumentException("Invalid sample size: " + n);
        }

        if (k < 0) {
            throw new IllegalArgumentException("Invalid number of bootstrap: " + k);
        }

        Split[] splits = new Split[k];

        for (int j = 0; j < k; j++) {
            boolean[] hit = new boolean[n];
            int hits = 0;

            int[] train = new int[n];
            for (int i = 0; i < n; i++) {
                int r = MathEx.randomInt(n);
                train[i] = r;
                if (!hit[r]) {
                    hits++;
                    hit[r] = true;
                }
            }

            int[] test = new int[n - hits];
            for (int i = 0, p = 0; i < n; i++) {
                if (!hit[i]) {
                    test[p++] = i;
                }
            }

            splits[j] = new Split(train, test);
        }

        return splits;
    }

    /**
     * Runs classification bootstrap validation.
     * @param k k-fold bootstrap sampling.
     * @param x the samples.
     * @param y the sample labels.
     * @param trainer the lambda to train a model.
     * @return the validation results.
     */
    static <T, M extends Classifier<T>> ClassificationValidations<M> classification(int k, T[] x, int[] y, BiFunction<T[], int[], M> trainer) {
        return ClassificationValidation.of(of(x.length, k), x, y, trainer);
    }

    /**
     * Runs classification bootstrap validation.
     * @param k k-fold bootstrap sampling.
     * @param formula the model specification.
     * @param data the training/validation data.
     * @param trainer the lambda to train a model.
     * @return the validation results.
     */
    static <M extends DataFrameClassifier> ClassificationValidations<M> classification(int k, Formula formula, DataFrame data, BiFunction<Formula, DataFrame, M> trainer) {
        return ClassificationValidation.of(of(data.size(), k), formula, data, trainer);
    }

    /**
     * Runs regression bootstrap validation.
     * @param k k-fold bootstrap sampling.
     * @param x the samples.
     * @param y the response variable.
     * @param trainer the lambda to train a model.
     * @return the validation results.
     */
    static <T, M extends Regression<T>> RegressionValidations<M> regression(int k, T[] x, double[] y, BiFunction<T[], double[], M> trainer) {
        return RegressionValidation.of(of(x.length, k), x, y, trainer);
    }

    /**
     * Runs regression bootstrap validation.
     * @param k k-fold bootstrap sampling.
     * @param formula the model specification.
     * @param data the training/validation data.
     * @param trainer the lambda to train a model.
     * @return the validation results.
     */
    static <M extends DataFrameRegression> RegressionValidations<M> regression(int k, Formula formula, DataFrame data, BiFunction<Formula, DataFrame, M> trainer) {
        return RegressionValidation.of(of(data.size(), k), formula, data, trainer);
    }
}

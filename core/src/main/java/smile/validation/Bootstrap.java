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

package smile.validation;

import smile.classification.Classifier;
import smile.data.DataFrame;
import smile.math.MathEx;
import smile.regression.Regression;

import java.util.function.BiFunction;
import java.util.function.Function;

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
public class Bootstrap {
    /**
     * The number of rounds of cross validation.
     */
    public final int k;
    /**
     * The index of training instances.
     */
    public final int[][] train;
    /**
     * The index of testing instances.
     */
    public final int[][] test;

    /**
     * Constructor.
     * @param n the number of samples.
     * @param k the number of rounds of bootstrap.
     */
    public Bootstrap(int n, int k) {
        if (n < 0) {
            throw new IllegalArgumentException("Invalid sample size: " + n);
        }

        if (k < 0) {
            throw new IllegalArgumentException("Invalid number of bootstrap: " + k);
        }

        this.k = k;
        train = new int[k][n];
        test = new int[k][];

        for (int j = 0; j < k; j++) {
            boolean[] hit = new boolean[n];
            int hits = 0;

            for (int i = 0; i < n; i++) {
                int r = MathEx.randomInt(n);
                train[j][i] = r;
                if (!hit[r]) {
                    hits++;
                    hit[r] = true;
                }
            }

            test[j] = new int[n - hits];
            for (int i = 0, p = 0; i < n; i++) {
                if (!hit[i]) {
                    test[j][p++] = i;
                }
            }
        }
    }

    /**
     * Runs cross validation tests.
     * @return the number of errors.
     */
    public <T> int classification(T[] x, int[] y, BiFunction<T[], int[], Classifier<T>> trainer) {
        int error = 0;

        for (int i = 0; i < k; i++) {
            T[] trainx = MathEx.slice(x, train[i]);
            int[] trainy = MathEx.slice(y, train[i]);
            T[] testx = MathEx.slice(x, test[i]);
            int[] testy = MathEx.slice(y, test[i]);

            Classifier<T> model = trainer.apply(trainx, trainy);
            for (int j = 0; j < testx.length; j++) {
                if (testy[j] != model.predict(testx[j])) error++;
            }
        }

        return error;
    }

    /**
     * Runs cross validation tests.
     * @return root mean squared errors.
     */
    public <T> int classification(DataFrame data, Function<DataFrame, Classifier<T>> trainer) {
        int error = 0;

        for (int i = 0; i < k; i++) {
            Classifier<T> model = trainer.apply(data.of(train[i]));
            DataFrame oob = data.of(test[i]);
            int[] prediction = model.predict(oob);
            int[] y = model.formula().get().response(oob).toIntArray();

            for (int j = 0; j < y.length; j++) {
                if (y[j] != prediction[j]) error++;
            }
        }

        return error;
    }

    /**
     * Runs bootstrap tests.
     * @return root mean squared error.
     */
    public <T> double regression(T[] x, double[] y, BiFunction<T[], double[], Regression<T>> trainer) {
        double rmse = 0.0;

        for (int i = 0; i < k; i++) {
            T[] trainx = MathEx.slice(x, train[i]);
            double[] trainy = MathEx.slice(y, train[i]);
            T[] testx = MathEx.slice(x, test[i]);
            double[] testy = MathEx.slice(y, test[i]);

            Regression<T> model = trainer.apply(trainx, trainy);
            for (int j = 0; j < testx.length; j++) {
                double r = testy[j] - model.predict(testx[j]);
                rmse += r * r;
            }
        }

        return Math.sqrt(rmse / x.length);
    }

    /**
     * Runs bootstrap tests.
     * @return root mean squared error.
     */
    public <T> double regression(DataFrame data, Function<DataFrame, Regression<T>> trainer) {
        double rmse = 0.0;

        for (int i = 0; i < k; i++) {
            Regression<T> model = trainer.apply(data.of(train[i]));
            DataFrame oob = data.of(test[i]);
            double[] prediction = model.predict(oob);
            double[] y = model.formula().get().response(oob).toDoubleArray();

            for (int j = 0; j < y.length; j++) {
                double r = y[j] - prediction[j];
                rmse += r * r;
            }
        }

        return Math.sqrt(rmse / data.size());
    }

    /**
     * Runs cross validation tests.
     * @return the number of errors.
     */
    public static <T> int classification(int k, T[] x, int[] y, BiFunction<T[], int[], Classifier<T>> trainer) {
        Bootstrap cv = new Bootstrap(x.length, k);
        return cv.classification(x, y, trainer);
    }

    /**
     * Runs cross validation tests.
     * @return the number of errors.
     */
    public static <T> int classification(int k, DataFrame data, Function<DataFrame, Classifier<T>> trainer) {
        Bootstrap cv = new Bootstrap(data.size(), k);
        return cv.classification(data, trainer);
    }

    /**
     * Runs bootstrap tests.
     * @return root mean squared error.
     */
    public static <T> double regression(int k, T[] x, double[] y, BiFunction<T[], double[], Regression<T>> trainer) {
        Bootstrap cv = new Bootstrap(x.length, k);
        return cv.regression(x, y, trainer);
    }

    /**
     * Runs bootstrap tests.
     * @return root mean squared error.
     */
    public static <T> double regression(int k, DataFrame data, Function<DataFrame, Regression<T>> trainer) {
        Bootstrap cv = new Bootstrap(data.size(), k);
        return cv.regression(data, trainer);
    }
}

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

import java.util.function.BiFunction;
import smile.math.MathEx;
import smile.classification.Classifier;
import smile.regression.Regression;
import smile.data.DataFrame;

/**
 * A utility class for validating predictive models on test data.
 * 
 * @author Haifeng
 */
public interface Validation {
    /**
     * Tests a classifier on a validation set.
     * 
     * @param <T> the data type of input objects.
     * @param classifier a trained classifier to be tested.
     * @param x the test data set.
     * @param y the test data labels.
     * @return the accuracy on the test dataset
     */
    static <T> double test(Classifier<T> classifier, T[] x, int[] y) {
        int n = x.length;
        int[] predictions = new int[n];
        for (int i = 0; i < n; i++) {
            predictions[i] = classifier.predict(x[i]);
        }
        
        return new Accuracy().measure(y, predictions);
    }
    
    /**
     * Tests a regression model on a validation set.
     * 
     * @param <T> the data type of input objects.
     * @param regression a trained regression model to be tested.
     * @param x the test data set.
     * @param y the test data response values.
     * @return root mean squared error
     */
    static <T> double test(Regression<T> regression, T[] x, double[] y) {
        int n = x.length;
        double[] predictions = new double[n];
        for (int i = 0; i < n; i++) {
            predictions[i] = regression.predict(x[i]);
        }
        
        return new RMSE().measure(y, predictions);
    }

    /**
     * Tests a regression model on a validation set.
     *
     * @param <T> the data type of input objects.
     * @param model a trained regression model to be tested.
     * @param data the test data set.
     * @return root mean squared error
     */
    static <T> double test(Regression<T> model, DataFrame data) {
        double[] prediction = model.predict(data);
        double[] y = model.formula().get().response(data).toDoubleArray();
        return RMSE.instance.measure(y, prediction);
    }

    /**
     * Tests a classifier on a validation set.
     * 
     * @param <T> the data type of input objects.
     * @param classifier a trained classifier to be tested.
     * @param x the test data set.
     * @param y the test data labels.
     * @param measure the performance measures of classification.
     * @return the test results with the same size of order of measures
     */
    static <T> double test(Classifier<T> classifier, T[] x, int[] y, ClassificationMeasure measure) {
        int n = x.length;
        int[] predictions = new int[n];
        for (int i = 0; i < n; i++) {
            predictions[i] = classifier.predict(x[i]);
        }
        
        return measure.measure(y, predictions);
    }
    
    /**
     * Tests a classifier on a validation set.
     * 
     * @param <T> the data type of input objects.
     * @param classifier a trained classifier to be tested.
     * @param x the test data set.
     * @param y the test data labels.
     * @param measures the performance measures of classification.
     * @return the test results with the same size of order of measures
     */
    static <T> double[] test(Classifier<T> classifier, T[] x, int[] y, ClassificationMeasure[] measures) {
        int n = x.length;
        int[] predictions = new int[n];
        for (int i = 0; i < n; i++) {
            predictions[i] = classifier.predict(x[i]);
        }
        
        int m = measures.length;
        double[] results = new double[m];
        for (int i = 0; i < m; i++) {
            results[i] = measures[i].measure(y, predictions);
        }
        
        return results;
    }
    
    /**
     * Tests a regression model on a validation set.
     * 
     * @param <T> the data type of input objects.
     * @param regression a trained regression model to be tested.
     * @param x the test data set.
     * @param y the test data response values.
     * @param measure the performance measure of regression.
     * @return the test results with the same size of order of measures
     */
    static <T> double test(Regression<T> regression, T[] x, double[] y, RegressionMeasure measure) {
        int n = x.length;
        double[] predictions = new double[n];
        for (int i = 0; i < n; i++) {
            predictions[i] = regression.predict(x[i]);
        }
        
        return measure.measure(y, predictions);
    }
    
    /**
     * Tests a regression model on a validation set.
     * 
     * @param <T> the data type of input objects.
     * @param regression a trained regression model to be tested.
     * @param x the test data set.
     * @param y the test data response values.
     * @param measures the performance measures of regression.
     * @return the test results with the same size of order of measures
     */
    static <T> double[] test(Regression<T> regression, T[] x, double[] y, RegressionMeasure[] measures) {
        int n = x.length;
        double[] predictions = new double[n];
        for (int i = 0; i < n; i++) {
            predictions[i] = regression.predict(x[i]);
        }
        
        int m = measures.length;
        double[] results = new double[m];
        for (int i = 0; i < m; i++) {
            results[i] = measures[i].measure(y, predictions);
        }
        
        return results;
    }
    
    /**
     * Leave-one-out cross validation of a classification model.
     * 
     * @param <T> the data type of input objects.
     * @param trainer a classifier trainer that is properly parameterized.
     * @param x the test data set.
     * @param y the test data labels.
     * @return the accuracy on test dataset
     */
    static <T> double loocv(BiFunction<T[], int[], Classifier<T>> trainer, T[] x, int[] y) {
        int m = 0;
        int n = x.length;
        
        LOOCV loocv = new LOOCV(n);
        for (int i = 0; i < n; i++) {
            T[] trainx = MathEx.slice(x, loocv.train[i]);
            int[] trainy = MathEx.slice(y, loocv.train[i]);
            
            Classifier<T> classifier = trainer.apply(trainx, trainy);

            if (classifier.predict(x[loocv.test[i]]) == y[loocv.test[i]]) {
                m++;
            }
        }
        
        return (double) m / n;
    }
    
    /**
     * Leave-one-out cross validation of a regression model.
     * 
     * @param <T> the data type of input objects.
     * @param trainer a regression model trainer that is properly parameterized.
     * @param x the test data set.
     * @param y the test data response values.
     * @return root mean squared error
     */
    static <T> double loocv(BiFunction<T[], double[], Regression<T>> trainer, T[] x, double[] y) {
        double rmse = 0.0;
        int n = x.length;        
        LOOCV loocv = new LOOCV(n);
        for (int i = 0; i < n; i++) {
            T[] trainx = MathEx.slice(x, loocv.train[i]);
            double[] trainy = MathEx.slice(y, loocv.train[i]);
            
            Regression<T> model = trainer.apply(trainx, trainy);

            rmse += MathEx.sqr(model.predict(x[loocv.test[i]]) - y[loocv.test[i]]);
        }
        
        return Math.sqrt(rmse / n);
    }
    
    /**
     * Leave-one-out cross validation of a classification model.
     * 
     * @param <T> the data type of input objects.
     * @param trainer a classifier trainer that is properly parameterized.
     * @param x the test data set.
     * @param y the test data labels.
     * @param measure the performance measure of classification.
     * @return the test results with the same size of order of measures
     */
    static <T> double loocv(BiFunction<T[], int[], Classifier<T>> trainer, T[] x, int[] y, ClassificationMeasure measure) {
        int n = x.length;
        int[] predictions = new int[n];
        
        LOOCV loocv = new LOOCV(n);
        for (int i = 0; i < n; i++) {
            T[] trainx = MathEx.slice(x, loocv.train[i]);
            int[] trainy = MathEx.slice(y, loocv.train[i]);
            
            Classifier<T> classifier = trainer.apply(trainx, trainy);

            predictions[loocv.test[i]] = classifier.predict(x[loocv.test[i]]);
        }
        
        return measure.measure(y, predictions);
    }
    
    /**
     * Leave-one-out cross validation of a classification model.
     * 
     * @param <T> the data type of input objects.
     * @param trainer a classifier trainer that is properly parameterized.
     * @param x the test data set.
     * @param y the test data labels.
     * @param measures the performance measures of classification.
     * @return the test results with the same size of order of measures
     */
    static <T> double[] loocv(BiFunction<T[], int[], Classifier<T>> trainer, T[] x, int[] y, ClassificationMeasure[] measures) {
        int n = x.length;
        int[] predictions = new int[n];
        
        LOOCV loocv = new LOOCV(n);
        for (int i = 0; i < n; i++) {
            T[] trainx = MathEx.slice(x, loocv.train[i]);
            int[] trainy = MathEx.slice(y, loocv.train[i]);
            
            Classifier<T> classifier = trainer.apply(trainx, trainy);

            predictions[loocv.test[i]] = classifier.predict(x[loocv.test[i]]);
        }
        
        int m = measures.length;
        double[] results = new double[m];
        for (int i = 0; i < m; i++) {
            results[i] = measures[i].measure(y, predictions);
        }
        
        return results;
    }
    
    /**
     * Leave-one-out cross validation of a regression model.
     * 
     * @param <T> the data type of input objects.
     * @param trainer a regression model trainer that is properly parameterized.
     * @param x the test data set.
     * @param y the test data response values.
     * @param measure the performance measure of regression.
     * @return the test results with the same size of order of measures
     */
    static <T> double loocv(BiFunction<T[], double[], Regression<T>> trainer, T[] x, double[] y, RegressionMeasure measure) {
        int n = x.length;
        double[] predictions = new double[n];
        
        LOOCV loocv = new LOOCV(n);
        for (int i = 0; i < n; i++) {
            T[] trainx = MathEx.slice(x, loocv.train[i]);
            double[] trainy = MathEx.slice(y, loocv.train[i]);
            
            Regression<T> model = trainer.apply(trainx, trainy);

            predictions[loocv.test[i]] = model.predict(x[loocv.test[i]]);
        }
        
        return measure.measure(y, predictions);
    }
    
    /**
     * Leave-one-out cross validation of a regression model.
     * 
     * @param <T> the data type of input objects.
     * @param trainer a regression model trainer that is properly parameterized.
     * @param x the test data set.
     * @param y the test data response values.
     * @param measures the performance measures of regression.
     * @return the test results with the same size of order of measures
     */
    static <T> double[] loocv(BiFunction<T[], double[], Regression<T>> trainer, T[] x, double[] y, RegressionMeasure[] measures) {
        int n = x.length;
        double[] predictions = new double[n];
        
        LOOCV loocv = new LOOCV(n);
        for (int i = 0; i < n; i++) {
            T[] trainx = MathEx.slice(x, loocv.train[i]);
            double[] trainy = MathEx.slice(y, loocv.train[i]);
            
            Regression<T> model = trainer.apply(trainx, trainy);

            predictions[loocv.test[i]] = model.predict(x[loocv.test[i]]);
        }
        
        int m = measures.length;
        double[] results = new double[m];
        for (int i = 0; i < m; i++) {
            results[i] = measures[i].measure(y, predictions);
        }
        
        return results;
    }
    
    /**
     * Cross validation of a classification model.
     * 
     * @param <T> the data type of input objects.
     * @param k k-fold cross validation.
     * @param trainer a classifier trainer that is properly parameterized.
     * @param x the test data set.
     * @param y the test data labels.
     * @return the accuracy on test dataset
     */
    static <T> double cv(int k, BiFunction<T[], int[], Classifier<T>> trainer, T[] x, int[] y) {
        if (k < 2) {
            throw new IllegalArgumentException("Invalid k for k-fold cross validation: " + k);
        }
        
        int n = x.length;
        int[] predictions = new int[n];
        
        CrossValidation cv = new CrossValidation(n, k);
        for (int i = 0; i < k; i++) {
            T[] trainx = MathEx.slice(x, cv.train[i]);
            int[] trainy = MathEx.slice(y, cv.train[i]);
            
            Classifier<T> classifier = trainer.apply(trainx, trainy);

            for (int j : cv.test[i]) {
                predictions[j] = classifier.predict(x[j]);
            }
        }
        
        return new Accuracy().measure(y, predictions);
    }
    
    /**
     * Cross validation of a regression model.
     * 
     * @param <T> the data type of input objects.
     * @param k k-fold cross validation.
     * @param trainer a regression model trainer that is properly parameterized.
     * @param x the test data set.
     * @param y the test data response values.
     * @return root mean squared error
     */
    static <T> double cv(int k, BiFunction<T[], double[], Regression<T>> trainer, T[] x, double[] y) {
        if (k < 2) {
            throw new IllegalArgumentException("Invalid k for k-fold cross validation: " + k);
        }
        
        int n = x.length;
        double[] predictions = new double[n];
        
        CrossValidation cv = new CrossValidation(n, k);
        for (int i = 0; i < k; i++) {
            T[] trainx = MathEx.slice(x, cv.train[i]);
            double[] trainy = MathEx.slice(y, cv.train[i]);
            
            Regression<T> model = trainer.apply(trainx, trainy);

            for (int j : cv.test[i]) {
                predictions[j] = model.predict(x[j]);
            }
        }
        
        return new RMSE().measure(y, predictions);
    }
    
    /**
     * Cross validation of a classification model.
     * 
     * @param <T> the data type of input objects.
     * @param k k-fold cross validation.
     * @param trainer a classifier trainer that is properly parameterized.
     * @param x the test data set.
     * @param y the test data labels.
     * @param measure the performance measure of classification.
     * @return the test results with the same size of order of measures
     */
    static <T> double cv(int k, BiFunction<T[], int[], Classifier<T>> trainer, T[] x, int[] y, ClassificationMeasure measure) {
        if (k < 2) {
            throw new IllegalArgumentException("Invalid k for k-fold cross validation: " + k);
        }
        
        int n = x.length;
        int[] predictions = new int[n];
        
        CrossValidation cv = new CrossValidation(n, k);
        for (int i = 0; i < k; i++) {
            T[] trainx = MathEx.slice(x, cv.train[i]);
            int[] trainy = MathEx.slice(y, cv.train[i]);
            
            Classifier<T> classifier = trainer.apply(trainx, trainy);

            for (int j : cv.test[i]) {
                predictions[j] = classifier.predict(x[j]);
            }
        }
        
        return measure.measure(y, predictions);
    }
    
    /**
     * Cross validation of a classification model.
     * 
     * @param <T> the data type of input objects.
     * @param k k-fold cross validation.
     * @param trainer a classifier trainer that is properly parameterized.
     * @param x the test data set.
     * @param y the test data labels.
     * @param measures the performance measures of classification.
     * @return the test results with the same size of order of measures
     */
    static <T> double[] cv(int k, BiFunction<T[], int[], Classifier<T>> trainer, T[] x, int[] y, ClassificationMeasure[] measures) {
        if (k < 2) {
            throw new IllegalArgumentException("Invalid k for k-fold cross validation: " + k);
        }
        
        int n = x.length;
        int[] predictions = new int[n];
        
        CrossValidation cv = new CrossValidation(n, k);
        for (int i = 0; i < k; i++) {
            T[] trainx = MathEx.slice(x, cv.train[i]);
            int[] trainy = MathEx.slice(y, cv.train[i]);
            
            Classifier<T> classifier = trainer.apply(trainx, trainy);

            for (int j : cv.test[i]) {
                predictions[j] = classifier.predict(x[j]);
            }
        }
        
        int m = measures.length;
        double[] results = new double[m];
        for (int i = 0; i < m; i++) {
            results[i] = measures[i].measure(y, predictions);
        }
        
        return results;
    }
    
    /**
     * Cross validation of a regression model.
     * 
     * @param <T> the data type of input objects.
     * @param k k-fold cross validation.
     * @param trainer a regression model trainer that is properly parameterized.
     * @param x the test data set.
     * @param y the test data response values.
     * @param measure the performance measure of regression.
     * @return the test results with the same size of order of measures
     */
    static <T> double cv(int k, BiFunction<T[], double[], Regression<T>> trainer, T[] x, double[] y, RegressionMeasure measure) {
        if (k < 2) {
            throw new IllegalArgumentException("Invalid k for k-fold cross validation: " + k);
        }
        
        int n = x.length;
        double[] predictions = new double[n];
        
        CrossValidation cv = new CrossValidation(n, k);
        for (int i = 0; i < k; i++) {
            T[] trainx = MathEx.slice(x, cv.train[i]);
            double[] trainy = MathEx.slice(y, cv.train[i]);
            
            Regression<T> model = trainer.apply(trainx, trainy);

            for (int j : cv.test[i]) {
                predictions[j] = model.predict(x[j]);
            }
        }
        
        return measure.measure(y, predictions);
    }
    
    /**
     * Cross validation of a regression model.
     * 
     * @param <T> the data type of input objects.
     * @param k k-fold cross validation.
     * @param trainer a regression model trainer that is properly parameterized.
     * @param x the test data set.
     * @param y the test data response values.
     * @param measures the performance measures of regression.
     * @return the test results with the same size of order of measures
     */
    static <T> double[] cv(int k, BiFunction<T[], double[], Regression<T>> trainer, T[] x, double[] y, RegressionMeasure[] measures) {
        if (k < 2) {
            throw new IllegalArgumentException("Invalid k for k-fold cross validation: " + k);
        }
        
        int n = x.length;
        double[] predictions = new double[n];
        
        CrossValidation cv = new CrossValidation(n, k);
        for (int i = 0; i < k; i++) {
            T[] trainx = MathEx.slice(x, cv.train[i]);
            double[] trainy = MathEx.slice(y, cv.train[i]);
            
            Regression<T> model = trainer.apply(trainx, trainy);

            for (int j : cv.test[i]) {
                predictions[j] = model.predict(x[j]);
            }
        }
        
        int m = measures.length;
        double[] results = new double[m];
        for (int i = 0; i < m; i++) {
            results[i] = measures[i].measure(y, predictions);
        }
        
        return results;
    }
    
    /**
     * Bootstrap accuracy estimation of a classification model.
     * 
     * @param <T> the data type of input objects.
     * @param k k-round bootstrap estimation.
     * @param trainer a classifier trainer that is properly parameterized.
     * @param x the test data set.
     * @param y the test data labels.
     * @return the k-round accuracies
     */
    static <T> double[] bootstrap(int k, BiFunction<T[], int[], Classifier<T>> trainer, T[] x, int[] y) {
        if (k < 2) {
            throw new IllegalArgumentException("Invalid k for k-fold bootstrap: " + k);
        }
        
        int n = x.length;
        double[] results = new double[k];
        Accuracy measure = new Accuracy();
        
        Bootstrap bootstrap = new Bootstrap(n, k);
        for (int i = 0; i < k; i++) {
            T[] trainx = MathEx.slice(x, bootstrap.train[i]);
            int[] trainy = MathEx.slice(y, bootstrap.train[i]);
            
            Classifier<T> classifier = trainer.apply(trainx, trainy);

            int nt = bootstrap.test[i].length;
            int[] truth = new int[nt];
            int[] predictions = new int[nt];
            for (int j = 0; j < nt; j++) {
                int l = bootstrap.test[i][j];
                truth[j] = y[l];
                predictions[j] = classifier.predict(x[l]);
            }

            results[i] = measure.measure(truth, predictions);
        }
        
        return results;
    }
    
    /**
     * Bootstrap RMSE estimation of a regression model.
     * 
     * @param <T> the data type of input objects.
     * @param k k-round bootstrap estimation.
     * @param trainer a regression model trainer that is properly parameterized.
     * @param x the test data set.
     * @param y the test data response values.
     * @return the k-round root mean squared errors
     */
    static <T> double[] bootstrap(int k, BiFunction<T[], double[], Regression<T>> trainer, T[] x, double[] y) {
        if (k < 2) {
            throw new IllegalArgumentException("Invalid k for k-fold bootstrap: " + k);
        }
        
        int n = x.length;
        double[] results = new double[k];
        RMSE measure = new RMSE();
        
        Bootstrap bootstrap = new Bootstrap(n, k);
        for (int i = 0; i < k; i++) {
            T[] trainx = MathEx.slice(x, bootstrap.train[i]);
            double[] trainy = MathEx.slice(y, bootstrap.train[i]);
            
            Regression<T> model = trainer.apply(trainx, trainy);

            int nt = bootstrap.test[i].length;
            double[] truth = new double[nt];
            double[] predictions = new double[nt];
            for (int j = 0; j < nt; j++) {
                int l = bootstrap.test[i][j];
                truth[j] = y[l];
                predictions[j] = model.predict(x[l]);
            }

            results[i] = measure.measure(truth, predictions);
        }
        
        return results;
    }
    
    /**
     * Bootstrap performance estimation of a classification model.
     * 
     * @param <T> the data type of input objects.
     * @param k k-fold bootstrap estimation.
     * @param trainer a classifier trainer that is properly parameterized.
     * @param x the test data set.
     * @param y the test data labels.
     * @param measure the performance measures of classification.
     * @return k-by-m test result matrix, where k is the number of
     * bootstrap samples and m is the number of performance measures.
     */
    static <T> double[] bootstrap(int k, BiFunction<T[], int[], Classifier<T>>trainer, T[] x, int[] y, ClassificationMeasure measure) {
        if (k < 2) {
            throw new IllegalArgumentException("Invalid k for k-fold bootstrap: " + k);
        }
        
        int n = x.length;
        double[] results = new double[k];
        
        Bootstrap bootstrap = new Bootstrap(n, k);
        for (int i = 0; i < k; i++) {
            T[] trainx = MathEx.slice(x, bootstrap.train[i]);
            int[] trainy = MathEx.slice(y, bootstrap.train[i]);
            
            Classifier<T> classifier = trainer.apply(trainx, trainy);

            int nt = bootstrap.test[i].length;
            int[] truth = new int[nt];
            int[] predictions = new int[nt];
            for (int j = 0; j < nt; j++) {
                int l = bootstrap.test[i][j];
                truth[j] = y[l];
                predictions[j] = classifier.predict(x[l]);
            }

            results[i] = measure.measure(truth, predictions);
        }
        
        return results;
    }
    
    /**
     * Bootstrap performance estimation of a classification model.
     * 
     * @param <T> the data type of input objects.
     * @param k k-fold bootstrap estimation.
     * @param trainer a classifier trainer that is properly parameterized.
     * @param x the test data set.
     * @param y the test data labels.
     * @param measures the performance measures of classification.
     * @return k-by-m test result matrix, where k is the number of
     * bootstrap samples and m is the number of performance measures.
     */
    static <T> double[][] bootstrap(int k, BiFunction<T[], int[], Classifier<T>> trainer, T[] x, int[] y, ClassificationMeasure[] measures) {
        if (k < 2) {
            throw new IllegalArgumentException("Invalid k for k-fold bootstrap: " + k);
        }
        
        int n = x.length;
        int m = measures.length;
        double[][] results = new double[k][m];
        
        Bootstrap bootstrap = new Bootstrap(n, k);
        for (int i = 0; i < k; i++) {
            T[] trainx = MathEx.slice(x, bootstrap.train[i]);
            int[] trainy = MathEx.slice(y, bootstrap.train[i]);
            
            Classifier<T> classifier = trainer.apply(trainx, trainy);

            int nt = bootstrap.test[i].length;
            int[] truth = new int[nt];
            int[] predictions = new int[nt];
            for (int j = 0; j < nt; j++) {
                int l = bootstrap.test[i][j];
                truth[j] = y[l];
                predictions[j] = classifier.predict(x[l]);
            }

            for (int j = 0; j < m; j++) {
                results[i][j] = measures[j].measure(truth, predictions);
            }
        }
        
        return results;
    }
    
    /**
     * Bootstrap performance estimation of a regression model.
     * 
     * @param <T> the data type of input objects.
     * @param k k-fold bootstrap estimation.
     * @param trainer a regression model trainer that is properly parameterized.
     * @param x the test data set.
     * @param y the test data response values.
     * @param measure the performance measure of regression.
     * @return k-by-m test result matrix, where k is the number of 
     * bootstrap samples and m is the number of performance measures.
     */
    static <T> double[] bootstrap(int k, BiFunction<T[], double[], Regression<T>> trainer, T[] x, double[] y, RegressionMeasure measure) {
        if (k < 2) {
            throw new IllegalArgumentException("Invalid k for k-fold bootstrap: " + k);
        }
        
        int n = x.length;
        double[] results = new double[k];
        
        Bootstrap bootstrap = new Bootstrap(n, k);
        for (int i = 0; i < k; i++) {
            T[] trainx = MathEx.slice(x, bootstrap.train[i]);
            double[] trainy = MathEx.slice(y, bootstrap.train[i]);
            
            Regression<T> model = trainer.apply(trainx, trainy);

            int nt = bootstrap.test[i].length;
            double[] truth = new double[nt];
            double[] predictions = new double[nt];
            for (int j = 0; j < nt; j++) {
                int l = bootstrap.test[i][j];
                truth[j] = y[l];
                predictions[j] = model.predict(x[l]);
            }

            results[i] = measure.measure(truth, predictions);
        }
        
        return results;
    }
    
    /**
     * Bootstrap performance estimation of a regression model.
     * 
     * @param <T> the data type of input objects.
     * @param k k-fold bootstrap estimation.
     * @param trainer a regression model trainer that is properly parameterized.
     * @param x the test data set.
     * @param y the test data response values.
     * @param measures the performance measures of regression.
     * @return k-by-m test result matrix, where k is the number of 
     * bootstrap samples and m is the number of performance measures.
     */
    static <T> double[][] bootstrap(int k, BiFunction<T[], double[], Regression<T>> trainer, T[] x, double[] y, RegressionMeasure[] measures) {
        if (k < 2) {
            throw new IllegalArgumentException("Invalid k for k-fold bootstrap: " + k);
        }
        
        int n = x.length;
        int m = measures.length;
        double[][] results = new double[k][m];
        
        Bootstrap bootstrap = new Bootstrap(n, k);
        for (int i = 0; i < k; i++) {
            T[] trainx = MathEx.slice(x, bootstrap.train[i]);
            double[] trainy = MathEx.slice(y, bootstrap.train[i]);
            
            Regression<T> model = trainer.apply(trainx, trainy);

            int nt = bootstrap.test[i].length;
            double[] truth = new double[nt];
            double[] predictions = new double[nt];
            for (int j = 0; j < nt; j++) {
                int l = bootstrap.test[i][j];
                truth[j] = y[l];
                predictions[j] = model.predict(x[l]);
            }

            for (int j = 0; j < m; j++) {
                results[i][j] = measures[j].measure(truth, predictions);
            }
        }
        
        return results;
    }
}

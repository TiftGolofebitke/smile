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

package smile.classification;

import java.util.Arrays;
import java.util.Properties;
import java.util.stream.IntStream;

import smile.data.DataFrame;
import smile.data.formula.Formula;
import smile.data.type.StructType;
import smile.math.MathEx;
import smile.math.DifferentiableMultivariateFunction;
import smile.math.BFGS;
import smile.math.matrix.DenseMatrix;
import smile.math.matrix.Matrix;
import smile.math.special.Erf;
import smile.util.IntSet;
import smile.stat.Hypothesis;
import smile.validation.ModelSelection;

/**
 * Logistic regression. Logistic regression (logit model) is a generalized
 * linear model used for binomial regression. Logistic regression applies
 * maximum likelihood estimation after transforming the dependent into
 * a logit variable. A logit is the natural log of the odds of the dependent
 * equaling a certain value or not (usually 1 in binary logistic models,
 * the highest value in multinomial models). In this way, logistic regression
 * estimates the odds of a certain event (value) occurring. 
 * <p>
 * Goodness-of-fit tests such as the likelihood ratio test are available
 * as indicators of model appropriateness, as is the Wald statistic to test
 * the significance of individual independent variables. 
 * <p>
 * Logistic regression has many analogies to ordinary least squares (OLS)
 * regression. Unlike OLS regression, however, logistic regression does not
 * assume linearity of relationship between the raw values of the independent
 * variables and the dependent, does not require normally distributed variables,
 * does not assume homoscedasticity, and in general has less stringent
 * requirements.
 * <p>
 * Compared with linear discriminant analysis, logistic regression has several
 * advantages:
 * <ul>
 * <li> It is more robust: the independent variables don't have to be normally
 * distributed, or have equal variance in each group
 * <li> It does not assume a linear relationship between the independent
 * variables and dependent variable.
 * <li> It may handle nonlinear effects since one can add explicit interaction
 * and power terms.
 * </ul>
 * However, it requires much more data to achieve stable, meaningful results.
 * <p>
 * Logistic regression also has strong connections with neural network and
 * maximum entropy modeling. For example, binary logistic regression is
 * equivalent to a one-layer, single-output neural network with a logistic
 * activation function trained under log loss. Similarly, multinomial logistic
 * regression is equivalent to a one-layer, softmax-output neural network.
 * <p>
 * Logistic regression estimation also obeys the maximum entropy principle, and
 * thus logistic regression is sometimes called "maximum entropy modeling",
 * and the resulting classifier the "maximum entropy classifier".
 * 
 * @see MLP
 * @see Maxent
 * @see LDA
 * 
 * @author Haifeng Li
 */
public abstract class LogisticRegression implements SoftClassifier<double[]>, OnlineClassifier<double[]> {
    private static final long serialVersionUID = 2L;
    /**
     * The names of independent variables.
     */
    String[] fields;

    /**
     * The dimension of input space.
     */
    int p;

    /**
     * The number of classes.
     */
    int k;

    /**
     * The number of training samples.
     */
    int n;

    /**
     * The log-likelihood of learned model.
     */
    double L;

    /**
     * Regularization factor.
     */
    double lambda;

    /**
     * learning rate for stochastic gradient descent.
     */
    double eta = 0.1;

    /**
     * The class label encoder.
     */
    final IntSet labels;

    /**
     * Constructor.
     * @param p the dimension of input data.
     * @param L the log-likelihood of learned model.
     * @param lambda &lambda; &gt; 0 gives a "regularized" estimate of linear
     *               weights which often has superior generalization performance,
     *               especially when the dimensionality is high.
     * @param labels class labels
     */
    public LogisticRegression(int p, double L, double lambda, IntSet labels) {
        this.k = labels.size();
        this.p = p;
        this.L = L;
        this.lambda = lambda;
        this.labels = labels;
        fields = new String[p+1];
        fields[p] = "Intercept";
        for (int i = 0; i < p; i++) {
            fields[i] = "x" + (i+1);
        }
    }

    /** Binomial logistic regression. The dependent variable is nominal of two levels. */
    public static class Binomial extends LogisticRegression {
        /**
         * The linear weights.
         */
        private double[] w;
        /**
         * The coefficients, their standard errors, z-scores, and p-values.
         */
        double[][] ztest;
        /**
         * The fitted values.
         */
        double[] fittedValues;
        /**
         * The null hypothesis deviance.
         */
        double nullDeviance;
        /**
         * The residual deviance.
         */
        double residualDeviance;
        /**
         * The deviance residuals.
         */
        double[] residuals;
        /**
         * The degrees of freedom of the residual deviance.
         */
        int df;

        /**
         * Constructor.
         * @param w the weights.
         * @param L the log-likelihood of learned model.
         * @param lambda &lambda; &gt; 0 gives a "regularized" estimate of linear
         *               weights which often has superior generalization performance,
         *               especially when the dimensionality is high.
         * @param labels class labels
         */
        public Binomial(double[] w, double L, double lambda, IntSet labels) {
            super(w.length - 1, L, lambda, labels);
            this.w = w;
        }

        /**
         * Returns an array of size (p+1) containing the linear weights
         * of binary logistic regression, where p is the dimension of
         * feature vectors. The last element is the weight of bias.
         */
        public double[] coefficients() {
            return w;
        }

        /**
         * Returns the z-test of the coefficients (including intercept).
         * The first column is the coefficients, the second column is the standard
         * error of coefficients, the third column is the z-score of the hypothesis
         * test if the coefficient is zero, the fourth column is the p-values of
         * test. The last row is of intercept.
         */
        public double[][] ztest() {
            return ztest;
        }

        /**
         * Returns the deviance residuals.
         */
        public double[] residuals() {
            return residuals;
        }

        /**
         * Returns the fitted values.
         */
        public double[] fittedValues() {
            return fittedValues;
        }

        @Override
        public int predict(double[] x) {
            double f = 1.0 / (1.0 + Math.exp(-dot(x, w)));
            return labels.valueOf(f < 0.5 ? 0 : 1);
        }

        @Override
        public int predict(double[] x, double[] posteriori) {
            if (x.length != p) {
                throw new IllegalArgumentException(String.format("Invalid input vector size: %d, expected: %d", x.length, p));
            }

            if (posteriori.length != k) {
                throw new IllegalArgumentException(String.format("Invalid posteriori vector size: %d, expected: %d", posteriori.length, k));
            }

            double f = 1.0 / (1.0 + Math.exp(-dot(x, w)));

            posteriori[0] = 1.0 - f;
            posteriori[1] = f;

            return labels.valueOf(f < 0.5 ? 0 : 1);
        }

        @Override
        public void update(double[] x, int y) {
            if (x.length != p) {
                throw new IllegalArgumentException("Invalid input vector size: " + x.length);
            }

            y = labels.indexOf(y);

            // calculate gradient for incoming data
            double wx = dot(x, w);
            double err = y - MathEx.logistic(wx);

            // update the weights
            w[p] += eta * err;
            for (int j = 0; j < p; j++) {
                w[j] += eta * err * x[j];
            }

            // add regularization part
            if (lambda > 0.0) {
                for (int j = 0; j < p; j++) {
                    w[j] -= eta * lambda * w[j];
                }
            }
        }

        @Override
        public String toString() {
            StringBuilder builder = new StringBuilder();
            builder.append("Logistic Regression:\n");

            double[] r = residuals.clone();
            builder.append("\nDeviance Residuals:\n");
            builder.append("       Min          1Q      Median          3Q         Max\n");
            builder.append(String.format("%10.4f  %10.4f  %10.4f  %10.4f  %10.4f%n", MathEx.min(r), MathEx.q1(r), MathEx.median(r), MathEx.q3(r), MathEx.max(r)));

            builder.append("\nCoefficients:\n");
            if (ztest != null) {
                builder.append("                  Estimate Std. Error    z value   Pr(>|z|)\n");
                if (ztest.length > p) {
                    builder.append(String.format("Intercept       %10.4f %10.4f %10.4f %10.4f %s%n", ztest[p][0], ztest[p][1], ztest[p][2], ztest[p][3], Hypothesis.significance(ztest[p][3])));
                } else {
                    builder.append(String.format("Intercept       %10.4f%n", w[p]));
                }

                for (int i = 0; i < p; i++) {
                    builder.append(String.format("%-15s %10.4f %10.4f %10.4f %10.4f %s%n", fields[i], ztest[i][0], ztest[i][1], ztest[i][2], ztest[i][3], Hypothesis.significance(ztest[i][3])));
                }

                builder.append("---------------------------------------------------------------------\n");
                builder.append("Significance codes:  0 '***' 0.001 '**' 0.01 '*' 0.05 '.' 0.1 ' ' 1\n");
            } else {
                builder.append(String.format("Intercept       %10.4f%n", w[p]));
                for (int i = 0; i < p; i++) {
                    builder.append(String.format("%-15s %10.4f%n", fields[i], w[i]));
                }
            }

            builder.append(String.format("%n    Null deviance: %.1f on %d degrees of freedom", nullDeviance, df+p));
            builder.append(String.format("%nResidual deviance: %.1f on %d degrees of freedom", residualDeviance, df));
            builder.append(String.format("%nAIC: %.4f     BIC: %.4f%n", AIC(), BIC()));

            return builder.toString();
        }
    }

    /** Multinomial logistic regression. The dependent variable is nominal with more than two levels. */
    public static class Multinomial extends LogisticRegression {
        /**
         * The linear weights.
         */
        private double[][] w;

        /**
         * Constructor.
         * @param w the weights.
         * @param L the log-likelihood of learned model.
         * @param lambda &lambda; &gt; 0 gives a "regularized" estimate of linear
         *               weights which often has superior generalization performance,
         *               especially when the dimensionality is high.
         * @param labels class labels
         */
        public Multinomial(double[][] w, double L, double lambda, IntSet labels) {
            super(w[0].length - 1, L, lambda, labels);
            this.w = w;
        }

        /**
         * Returns a 2d-array of size (k-1) x (p+1), containing the linear weights
         * of multi-class logistic regression, where k is the number of classes
         * and p is the dimension of feature vectors. The last element of each
         * row is the weight of bias.
         */
        public double[][] coefficients() {
            return w;
        }

        @Override
        public int predict(double[] x) {
            return predict(x, new double[k]);
        }

        @Override
        public int predict(double[] x, double[] posteriori) {
            if (x.length != p) {
                throw new IllegalArgumentException(String.format("Invalid input vector size: %d, expected: %d", x.length, p));
            }

            if (posteriori.length != k) {
                throw new IllegalArgumentException(String.format("Invalid posteriori vector size: %d, expected: %d", posteriori.length, k));
            }

            posteriori[k-1] = 0.0;
            for (int i = 0; i < k-1; i++) {
                posteriori[i] = dot(x, w[i]);
            }

            MathEx.softmax(posteriori);
            return labels.valueOf(MathEx.whichMax(posteriori));
        }

        @Override
        public void update(double[] x, int y) {
            if (x.length != p) {
                throw new IllegalArgumentException("Invalid input vector size: " + x.length);
            }

            y = labels.indexOf(y);

            double[] prob = new double[k];
            for (int j = 0; j < k-1; j++) {
                prob[j] = dot(x, w[j]);
            }

            MathEx.softmax(prob);

            // update the weights
            for (int i = 0; i < k-1; i++) {
                double[] wi = w[i];
                double err = (y == i ? 1.0 : 0.0) - prob[i];
                wi[p] += eta * err;
                for (int j = 0; j < p; j++) {
                    wi[j] += eta * err * x[j];
                }

                // add regularization part
                if (lambda > 0.0) {
                    for (int j = 0; j < p; j++) {
                        wi[j] -= eta * lambda * wi[j];
                    }
                }
            }
        }
    }

    /**
     * Fits binomial logistic regression.
     *
     * @param formula a symbolic description of the model to be fitted.
     * @param data the data frame of the explanatory and response variables.
     */
    public static Binomial binomial(Formula formula, DataFrame data) {
        return binomial(formula, data, new Properties());
    }

    /**
     * Fits binomial logistic regression.
     *
     * @param formula a symbolic description of the model to be fitted.
     * @param data the data frame of the explanatory and response variables.
     */
    public static Binomial binomial(Formula formula, DataFrame data, Properties prop) {
        DataFrame X = formula.x(data);
        double[][] x = X.toArray();
        int[] y = formula.y(data).toIntArray();
        Binomial model = binomial(x, y, prop);

        StructType schema = X.schema();
        int p = schema.length();
        for (int i = 0; i < p; i++) {
            model.fields[i] = schema.fieldName(i);;
        }

        return model;
    }

    /**
     * Fits binomial logistic regression.
     * @param x training samples.
     * @param y training labels.
     */
    public static Binomial binomial(double[][] x, int[] y) {
        return binomial(x, y, new Properties());
    }

    /**
     * Fits binomial logistic regression.
     * @param x training samples.
     * @param y training labels.
     */
    public static Binomial binomial(double[][] x, int[] y, Properties prop) {
        double lambda = Double.valueOf(prop.getProperty("smile.logit.lambda", "0.1"));
        boolean stderr = Boolean.valueOf(prop.getProperty("smile.logit.standard.error", "true"));
        double tol = Double.valueOf(prop.getProperty("smile.logit.tolerance", "1E-5"));
        int maxIter = Integer.valueOf(prop.getProperty("smile.logit.max.iterations", "500"));
        return binomial(x, y, lambda, stderr, tol, maxIter);
    }

    /**
     * Fits binomial logistic regression.
     * 
     * @param x training samples.
     * @param y training labels.
     * @param lambda &lambda; &gt; 0 gives a "regularized" estimate of linear
     *               weights which often has superior generalization performance,
     *               especially when the dimensionality is high.
     * @param stderr if true, compute the estimated standard errors of the estimate of parameters
     * @param tol the tolerance for stopping iterations.
     * @param maxIter the maximum number of iterations.
     */
    public static Binomial binomial(double[][] x, int[] y, double lambda, boolean stderr, double tol, int maxIter) {
        if (x.length != y.length) {
            throw new IllegalArgumentException(String.format("The sizes of X and Y don't match: %d != %d", x.length, y.length));
        }

        if (lambda < 0.0) {
            throw new IllegalArgumentException("Invalid regularization factor: " + lambda);
        }

        if (tol <= 0.0) {
            throw new IllegalArgumentException("Invalid tolerance: " + tol);            
        }
        
        if (maxIter <= 0) {
            throw new IllegalArgumentException("Invalid maximum number of iterations: " + maxIter);            
        }

        int p = x[0].length;
        ClassLabels codec = ClassLabels.fit(y);
        int k = codec.k;
        y = codec.y;

        if (k != 2) {
            throw new IllegalArgumentException("Fits binomial model on multi-class data.");
        }

        BFGS bfgs = new BFGS(tol, maxIter);
        BinomialObjective objective = new BinomialObjective(x, y, lambda);
        double[] w = new double[p + 1];
        double L = -bfgs.minimize(objective, 5, w);

        Binomial model = new Binomial(w, L, lambda, codec.labels);
        model.setLearningRate(0.1 / x.length);

        int n = x.length;
        model.n = n;

        double[] fittedValues = new double[n];
        double[] residuals = new double[n];
        model.fittedValues = fittedValues;
        model.residuals = residuals;

        int[] y2 = y;
        double[] g2 = new double[n]; // second partial derivatives of likelihood

        int pos = (int) MathEx.sum(y);
        double ybar = (double) pos / n;
        model.nullDeviance = -2.0 * (pos * Math.log(ybar) + (n - pos) * Math.log(1.0 - ybar));

        model.df = n - p - 1;
        model.residualDeviance = IntStream.range(0, n).parallel().mapToDouble(i -> {
            double e = Math.exp(dot(x[i], w));
            double e1 = 1.0 + e;
            double fittedValue = e / e1;;
            fittedValues[i] = fittedValue;
            double d = y2[i] == 0 ? -2.0 * Math.log(1.0 - fittedValue) : -2.0 * Math.log(fittedValue);
            residuals[i] = Math.signum(y2[i] - fittedValue) * Math.sqrt(d);

            g2[i] = e / (e1 * e1);
            return d;
        }).sum();

        if (stderr) {
            DenseMatrix XGX = Matrix.zeros(p + 1, p + 1);
            for (int i = 0; i < p; i++) {
                for (int j = 0; j < p; j++) {
                    double s = 0.0;
                    for (int l = 0; l < n; l++) {
                        s += x[l][i] * g2[l] * x[l][j];
                    }
                    XGX.set(i, j, s);
                }

                double s = 0.0;
                for (int l = 0; l < n; l++) {
                    s += x[l][i] * g2[l];
                }
                XGX.set(i, p, s);
            }

            for (int j = 0; j < p; j++) {
                double s = 0.0;
                for (int l = 0; l < n; l++) {
                    s += g2[l] * x[l][j];
                }
                XGX.set(p, j, s);
            }

            XGX.set(p, p, MathEx.sum(g2));
            XGX.setSymmetric(true);
            DenseMatrix inv = XGX.cholesky(true).inverse();

            double[][] ztest = new double[p + 1][4];
            model.ztest = ztest;
            for (int i = 0; i <= p; i++) {
                ztest[i][0] = w[i];
                ztest[i][1] = Math.sqrt(inv.get(i, i));
                ztest[i][2] = ztest[i][0] / ztest[i][1];
                ztest[i][3] = 2.0 - Erf.erfc(-0.707106781186547524 * Math.abs(ztest[i][2]));
            }
        }

        return model;
    }

    /**
     * Fits multinomial logistic regression.
     *
     * @param formula a symbolic description of the model to be fitted.
     * @param data the data frame of the explanatory and response variables.
     */
    public static Multinomial multinomial(Formula formula, DataFrame data) {
        return multinomial(formula, data, new Properties());
    }

    /**
     * Fits multinomial logistic regression.
     *
     * @param formula a symbolic description of the model to be fitted.
     * @param data the data frame of the explanatory and response variables.
     */
    public static Multinomial multinomial(Formula formula, DataFrame data, Properties prop) {
        DataFrame X = formula.x(data);
        double[][] x = X.toArray();
        int[] y = formula.y(data).toIntArray();
        Multinomial model = multinomial(x, y, prop);

        StructType schema = X.schema();
        int p = schema.length();
        for (int i = 0; i < p; i++) {
            model.fields[i] = schema.fieldName(i);;
        }

        return model;
    }

    /**
     * Fits multinomial logistic regression.
     * @param x training samples.
     * @param y training labels.
     */
    public static Multinomial multinomial(double[][] x, int[] y) {
        return multinomial(x, y, new Properties());
    }

    /**
     * Fits multinomial logistic regression.
     * @param x training samples.
     * @param y training labels.
     */
    public static Multinomial multinomial(double[][] x, int[] y, Properties prop) {
        double lambda = Double.valueOf(prop.getProperty("smile.logit.lambda", "0.1"));
        boolean stderr = Boolean.valueOf(prop.getProperty("smile.logit.standard.error", "true"));
        double tol = Double.valueOf(prop.getProperty("smile.logit.tolerance", "1E-5"));
        int maxIter = Integer.valueOf(prop.getProperty("smile.logit.max.iterations", "500"));
        return multinomial(x, y, lambda, tol, maxIter);
    }

    /**
     * Fits multinomial logistic regression.
     *
     * @param x training samples.
     * @param y training labels.
     * @param lambda &lambda; &gt; 0 gives a "regularized" estimate of linear
     *               weights which often has superior generalization performance,
     *               especially when the dimensionality is high.
     * @param tol the tolerance for stopping iterations.
     * @param maxIter the maximum number of iterations.
     */
    public static Multinomial multinomial(double[][] x, int[] y, double lambda, double tol, int maxIter) {
        if (x.length != y.length) {
            throw new IllegalArgumentException(String.format("The sizes of X and Y don't match: %d != %d", x.length, y.length));
        }

        if (lambda < 0.0) {
            throw new IllegalArgumentException("Invalid regularization factor: " + lambda);
        }

        if (tol <= 0.0) {
            throw new IllegalArgumentException("Invalid tolerance: " + tol);
        }

        if (maxIter <= 0) {
            throw new IllegalArgumentException("Invalid maximum number of iterations: " + maxIter);
        }

        int p = x[0].length;
        ClassLabels codec = ClassLabels.fit(y);
        int k = codec.k;
        y = codec.y;

        if (k <= 2) {
            throw new IllegalArgumentException("Fits multinomial model on binary class data.");
        }

        BFGS bfgs = new BFGS(tol, maxIter);
        MultinomialObjective objective = new MultinomialObjective(x, y, k, lambda);
        double[] w = new double[(k - 1) * (p + 1)];
        double L = -bfgs.minimize(objective, 5, w);

        double[][] W = new double[k-1][p+1];
        for (int i = 0, l = 0; i < k-1; i++) {
            for (int j = 0; j <= p; j++, l++) {
                W[i][j] = w[l];
            }
        }

        Multinomial model = new Multinomial(W, L, lambda, codec.labels);
        model.setLearningRate(0.1 / x.length);
        return model;
    }

    /**
     * Fits logistic regression.
     *
     * @param formula a symbolic description of the model to be fitted.
     * @param data the data frame of the explanatory and response variables.
     */
    public static LogisticRegression fit(Formula formula, DataFrame data) {
        return fit(formula, data, new Properties());
    }

    /**
     * Fits logistic regression.
     *
     * @param formula a symbolic description of the model to be fitted.
     * @param data the data frame of the explanatory and response variables.
     */
    public static LogisticRegression fit(Formula formula, DataFrame data, Properties prop) {
        DataFrame X = formula.x(data);
        double[][] x = X.toArray();
        int[] y = formula.y(data).toIntArray();
        LogisticRegression model = fit(x, y, prop);

        StructType schema = X.schema();
        int p = schema.length();
        for (int i = 0; i < p; i++) {
            model.fields[i] = schema.fieldName(i);;
        }

        return model;
    }

    /**
     * Fits logistic regression.
     * @param x training samples.
     * @param y training labels.
     */
    public static LogisticRegression fit(double[][] x, int[] y) {
        return fit(x, y, new Properties());
    }

    /**
     * Fits logistic regression.
     * @param x training samples.
     * @param y training labels.
     */
    public static LogisticRegression fit(double[][] x, int[] y, Properties prop) {
        double lambda = Double.valueOf(prop.getProperty("smile.logistic.lambda", "0.1"));
        double tol = Double.valueOf(prop.getProperty("smile.logistic.tolerance", "1E-5"));
        int maxIter = Integer.valueOf(prop.getProperty("smile.logistic.max.iterations", "500"));
        return fit(x, y, lambda, tol, maxIter);
    }

    /**
     * Fits logistic regression.
     *
     * @param x training samples.
     * @param y training labels.
     * @param lambda &lambda; &gt; 0 gives a "regularized" estimate of linear
     *               weights which often has superior generalization performance,
     *               especially when the dimensionality is high.
     * @param tol the tolerance for stopping iterations.
     * @param maxIter the maximum number of iterations.
     */
    public static LogisticRegression fit(double[][] x, int[] y, double lambda, double tol, int maxIter) {
        ClassLabels codec = ClassLabels.fit(y);
        if (codec.k == 2)
            return binomial(x, y, lambda, true, tol, maxIter);
        else
            return multinomial(x, y, lambda, tol, maxIter);
    }

    /**
     * Binary-class logistic regression objective function.
     */
    static class BinomialObjective implements DifferentiableMultivariateFunction {
        /**
         * Training instances.
         */
        double[][] x;
        /**
         * Training labels.
         */
        int[] y;
        /**
         * The dimension of feature space.
         */
        int p;
        /**
         * Regularization factor.
         */
        double lambda;
        /**
         * The number of samples in a partition.
         */
        int partitionSize;
        /**
         * The number of partitions.
         */
        int partitions;
        /**
         * The workspace to store gradient for each data partition.
         */
        double[][] gradients;

        /**
         * Constructor.
         */
        BinomialObjective(double[][] x, int[] y, double lambda) {
            this.x = x;
            this.y = y;
            this.lambda = lambda;
            this.p = x[0].length;

            partitionSize = Integer.valueOf(System.getProperty("smile.data.partition.size", "1000"));
            partitions = x.length / partitionSize + (x.length % partitionSize == 0 ? 0 : 1);
            gradients = new double[partitions][p+1];
        }

        @Override
        public double f(double[] w) {
            // Since BFGS try to minimize the objective function
            // and we try to maximize the log-likelihood, we really
            // return the negative log-likelihood here.
            double f = IntStream.range(0, x.length).parallel().mapToDouble(i -> {
                double wx = dot(x[i], w);
                return MathEx.log1pe(wx) - y[i] * wx;
            }).sum();

            if (lambda > 0.0) {
                double wnorm = 0.0;
                for (int i = 0; i < p; i++) wnorm += w[i] * w[i];
                f += 0.5 * lambda * wnorm;
            }

            return f;
        }

        @Override
        public double g(double[] w, double[] g) {
            double f = IntStream.range(0, partitions).parallel().mapToDouble(r -> {
                double[] gradient = gradients[r];
                Arrays.fill(gradient, 0.0);

                int begin = r * partitionSize;
                int end = (r + 1) * partitionSize;
                if (end > x.length) end = x.length;

                return IntStream.range(begin, end).sequential().mapToDouble(i -> {
                    double[] xi = x[i];
                    double wx = dot(xi, w);
                    double err = y[i] - MathEx.logistic(wx);
                    for (int j = 0; j < p; j++) {
                        gradient[j] -= err * xi[j];
                    }
                    gradient[p] -= err;

                    return MathEx.log1pe(wx) - y[i] * wx;
                }).sum();
            }).sum();

            Arrays.fill(g, 0.0);
            for (double[] gradient : gradients) {
                for (int i = 0; i < g.length; i++) {
                    g[i] += gradient[i];
                }
            }

            if (lambda > 0.0) {
                double wnorm = 0.0;
                for (int i = 0; i < p; i++) {
                    wnorm += w[i] * w[i];
                    g[i] += lambda * w[i];
                }
                f += 0.5 * lambda * wnorm;
            }

            return f;
        }
    }

    /**
     * Multi-class logistic regression objective function.
     */
    static class MultinomialObjective implements DifferentiableMultivariateFunction {
        /**
         * Training instances.
         */
        double[][] x;
        /**
         * Training labels.
         */
        int[] y;
        /**
         * The number of classes.
         */
        int k;
        /**
         * The dimension of feature space.
         */
        int p;
        /**
         * Regularization factor.
         */
        double lambda;
        /**
         * The number of samples in a partition.
         */
        int partitionSize;
        /**
         * The number of partitions.
         */
        int partitions;
        /**
         * The workspace to store gradient for each data partition.
         */
        double[][] gradients;
        /**
         * The workspace to store posteriori probability for each data partition.
         */
        double[][] posterioris;

        /**
         * Constructor.
         */
        MultinomialObjective(double[][] x, int[] y, int k, double lambda) {
            this.x = x;
            this.y = y;
            this.k = k;
            this.lambda = lambda;
            this.p = x[0].length;

            partitionSize = Integer.valueOf(System.getProperty("smile.data.partition.size", "1000"));
            partitions = x.length / partitionSize + (x.length % partitionSize == 0 ? 0 : 1);
            gradients = new double[partitions][(k-1)*(p+1)];
            posterioris = new double[partitions][k];
        }

        @Override
        public double f(double[] w) {
            double f = IntStream.range(0, partitions).parallel().mapToDouble(r -> {
                double[] posteriori = posterioris[r];

                int begin = r * partitionSize;
                int end = (r+1) * partitionSize;
                if (end > x.length) end = x.length;

                return IntStream.range(begin, end).sequential().mapToDouble(i -> {
                    posteriori[k - 1] = 0.0;
                    for (int j = 0; j < k - 1; j++) {
                        posteriori[j] = dot(x[i], w, j, p);
                    }

                    MathEx.softmax(posteriori);

                    return -MathEx.log(posteriori[y[i]]);
                }).sum();
            }).sum();

            if (lambda > 0.0) {
                double wnorm = 0.0;
                for (int i = 0; i < k-1; i++) {
                    for (int j = 0, pos = i * (p+1); j < p; j++) {
                        double wi = w[pos + j];
                        wnorm += wi * wi;
                    }
                }
                f += 0.5 * lambda * wnorm;
            }

            return f;
        }

        @Override
        public double g(double[] w, double[] g) {
            double f = IntStream.range(0, partitions).parallel().mapToDouble(r -> {
                        double[] posteriori = posterioris[r];
                        double[] gradient = gradients[r];
                        Arrays.fill(gradient, 0.0);

                        int begin = r * partitionSize;
                        int end = (r+1) * partitionSize;
                        if (end > x.length) end = x.length;

                        return IntStream.range(begin, end).sequential().mapToDouble(i -> {
                            posteriori[k - 1] = 0.0;
                            for (int j = 0; j < k - 1; j++) {
                                posteriori[j] = dot(x[i], w, j, p);
                            }

                            MathEx.softmax(posteriori);

                            for (int j = 0; j < k - 1; j++) {
                                double err = (y[i] == j ? 1.0 : 0.0) - posteriori[j];

                                int pos = j * (p + 1);
                                for (int l = 0; l < p; l++) {
                                    gradient[pos + l] -= err * x[i][l];
                                }
                                gradient[pos + p] -= err;
                            }

                            return -MathEx.log(posteriori[y[i]]);
                        }).sum();
            }).sum();

            Arrays.fill(g, 0.0);
            for (double[] gradient : gradients) {
                for (int i = 0; i < g.length; i++) {
                    g[i] += gradient[i];
                }
            }

            if (lambda > 0.0) {
                double wnorm = 0.0;
                for (int i = 0; i < k-1; i++) {
                    for (int j = 0, pos = i * (p+1); j < p; j++) {
                        double wi = w[pos + j];
                        wnorm += wi * wi;
                        g[pos + j] += lambda * wi;
                    }
                }
                f += 0.5 * lambda * wnorm;
            }

            return f;
        }
    }

    /**
     * Returns the dot product between weight vector and x (augmented with 1).
     */
    private static double dot(double[] x, double[] w) {
        double dot = w[x.length];

        for (int i = 0; i < x.length; i++) {
            dot += x[i] * w[i];
        }

        return dot;
    }

    /**
     * Returns the dot product between weight vector and x (augmented with 1).
     */
    private static double dot(double[] x, double[] w, int j, int p) {
        int pos = j * (p + 1);
        double dot = w[pos + p];

        for (int i = 0; i < p; i++) {
            dot += x[i] * w[pos+i];
        }

        return dot;
    }

    /**
     * Sets the learning rate of stochastic gradient descent.
     * It is a good practice to adapt the learning rate for
     * different data sizes. For example, it is typical to
     * set the learning rate to eta/n, where eta is in [0.1, 0.3]
     * and n is the size of the training data.
     *
     * @param rate the learning rate.
     */
    public void setLearningRate(double rate) {
        if (rate <= 0.0) {
            throw new IllegalArgumentException("Invalid learning rate: " + rate);
        }
        this.eta = rate;
    }

    /**
     * Returns the learning rate of stochastic gradient descent.
     */
    public double getLearningRate() {
        return eta;
    }

    /**
     * Returns the log-likelihood of model.
     */
    public double loglikelihood() {
        return L;
    }

    /**
     * Returns the AIC score.
     */
    public double AIC() {
        return ModelSelection.AIC(L, (k-1)*(p+1));
    }

    /**
     * Returns the BIC score.
     */
    public double BIC() {
        return ModelSelection.BIC(L, (k-1)*(p+1), n);
    }
}

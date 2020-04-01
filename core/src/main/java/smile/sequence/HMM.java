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

package smile.sequence;

import java.io.Serializable;
import java.util.Arrays;
import java.util.function.ToIntFunction;
import smile.math.MathEx;
import smile.math.matrix.DenseMatrix;
import smile.math.matrix.Matrix;
import smile.util.Strings;

/**
 * First-order Hidden Markov Model. A hidden Markov model (HMM) is a
 * statistical Markov model in which the system being modeled is assumed
 * to be a Markov process with unobserved (hidden) states. An HMM can be
 * considered as the simplest dynamic Bayesian network.
 *
 * In a regular Markov model, the state is directly visible to the observer,
 * and therefore the state transition probabilities are the only parameters.
 * In a hidden Markov model, the state is not directly visible, but output,
 * dependent on the state, is visible. Each state has a probability
 * distribution over the possible output tokens. Therefore the sequence of
 * tokens generated by an HMM gives some information about the sequence of
 * states.
 *
 * @author Haifeng Li
 */
public class HMM implements Serializable {
    private static final long serialVersionUID = 2L;

    /**
     * Initial state probabilities.
     */
    private double[] pi;
    /**
     * State transition probabilities.
     */
    private DenseMatrix a;
    /**
     * Symbol emission probabilities.
     */
    private DenseMatrix b;

    /**
     * Constructor.
     *
     * @param pi the initial state probabilities.
     * @param a the state transition probabilities, of which a[i][j]
     *          is P(s_j | s_i);
     * @param b the symbol emission probabilities, of which b[i][j]
     *          is P(o_j | s_i).
     */
    public HMM(double[] pi, DenseMatrix a, DenseMatrix b) {
        if (pi.length == 0) {
            throw new IllegalArgumentException("Invalid initial state probabilities.");
        }

        if (pi.length != a.nrows()) {
            throw new IllegalArgumentException("Invalid state transition probability matrix.");
        }

        if (a.nrows() != b.nrows()) {
            throw new IllegalArgumentException("Invalid symbol emission probability matrix.");
        }

        this.pi = pi;
        this.a = a;
        this.b = b;
    }

    /**
     * Returns the initial state probabilities.
     */
    public double[] getInitialStateProbabilities() {
        return pi;
    }

    /**
     * Returns the state transition probabilities.
     */
    public DenseMatrix getStateTransitionProbabilities() {
        return a;
    }

    /**
     * Returns the symbol emission probabilities.
     */
    public DenseMatrix getSymbolEmissionProbabilities() {
        return b;
    }

    /**
     * Returns the joint probability of an observation sequence along a state
     * sequence given this HMM.
     *
     * @param o an observation sequence.
     * @param s a state sequence.
     * @return the joint probability P(o, s | H) given the model H.
     */
    public double p(int[] o, int[] s) {
        return Math.exp(logp(o, s));
    }

    /**
     * Returns the log joint probability of an observation sequence along a
     * state sequence given this HMM.
     *
     * @param o an observation sequence.
     * @param s a state sequence.
     * @return the log joint probability P(o, s | H) given the model H.
     */
    public double logp(int[] o, int[] s) {
        if (o.length != s.length) {
            throw new IllegalArgumentException("The observation sequence and state sequence are not the same length.");
        }

        int n = s.length;
        double p = MathEx.log(pi[s[0]]) + MathEx.log(b.get(s[0], o[0]));
        for (int i = 1; i < n; i++) {
            p += MathEx.log(a.get(s[i - 1], s[i])) + MathEx.log(b.get(s[i], o[i]));
        }

        return p;
    }

    /**
     * Returns the probability of an observation sequence given this HMM.
     *
     * @param o an observation sequence.
     * @return the probability of this sequence.
     */
    public double p(int[] o) {
        return Math.exp(logp(o));
    }

    /**
     * Returns the logarithm probability of an observation sequence given this
     * HMM. A scaling procedure is used in order to avoid underflows when
     * computing the probability of long sequences.
     *
     * @param o an observation sequence.
     * @return the log probability of this sequence.
     */
    public double logp(int[] o) {
        double[][] alpha = new double[o.length][a.nrows()];
        double[] scaling = new double[o.length];

        forward(o, alpha, scaling);

        double p = 0.0;
        for (int t = 0; t < o.length; t++) {
            p += Math.log(scaling[t]);
        }

        return p;
    }

    /**
     * Normalize alpha[t] and put the normalization factor in scaling[t].
     */
    private void scale(double[] scaling, double[][] alpha, int t) {
        double[] table = alpha[t];

        double sum = 0.0;
        for (int i = 0; i < table.length; i++) {
            sum += table[i];
        }

        scaling[t] = sum;
        for (int i = 0; i < table.length; i++) {
            table[i] /= sum;
        }
    }

    /**
     * Scaled forward procedure without underflow.
     *
     * @param o an observation sequence.
     * @param alpha on output, alpha(i, j) holds the scaled total probability of
     * ending up in state i at time j.
     * @param scaling on output, it holds scaling factors.
     */
    private void forward(int[] o, double[][] alpha, double[] scaling) {
        int N = a.nrows();
        for (int k = 0; k < N; k++) {
            alpha[0][k] = pi[k] * b.get(k, o[0]);;
        }
        scale(scaling, alpha, 0);

        for (int t = 1; t < o.length; t++) {
            for (int k = 0; k < N; k++) {
                double sum = 0.0;

                for (int i = 0; i < N; i++) {
                    sum += alpha[t - 1][i] * a.get(i, k);
                }

                alpha[t][k] = sum * b.get(k, o[t]);
            }
            scale(scaling, alpha, t);
        }
    }

    /**
     * Scaled backward procedure without underflow.
     *
     * @param o an observation sequence.
     * @param beta on output, beta(i, j) holds the scaled total probability of
     * starting up in state i at time j.
     * @param scaling on input, it should hold scaling factors computed by
     * forward procedure.
     */
    private void backward(int[] o, double[][] beta, double[] scaling) {
        int N = a.nrows();
        int n = o.length - 1;
        for (int i = 0; i < N; i++) {
            beta[n][i] = 1.0 / scaling[n];
        }

        for (int t = n; t-- > 0;) {
            for (int i = 0; i < N; i++) {
                double sum = 0.;

                for (int j = 0; j < N; j++) {
                    sum += beta[t + 1][j] * a.get(i, j) * b.get(j, o[t + 1]);
                }

                beta[t][i] = sum / scaling[t];
            }
        }
    }

    /**
     * Returns the most likely state sequence given the observation sequence by
     * the Viterbi algorithm, which maximizes the probability of
     * <code>P(I | O, HMM)</code>. In the calculation, we may get ties. In this
     * case, one of them is chosen randomly.
     *
     * @param o an observation sequence.
     * @return the most likely state sequence.
     */
    public int[] predict(int[] o) {
        int N = a.nrows();
        // The probability of the most probable path.
        double[][] trellis = new double[o.length][N];
        // Backtrace.
        int[][] psy = new int[o.length][N];
        // The most likely state sequence.
        int[] s = new int[o.length];

        // forward
        for (int i = 0; i < N; i++) {
            trellis[0][i] = MathEx.log(pi[i]) + MathEx.log(b.get(i, o[0]));
            psy[0][i] = 0;
        }

        for (int t = 1; t < o.length; t++) {
            for (int j = 0; j < N; j++) {
                double maxDelta = Double.NEGATIVE_INFINITY;
                int maxPsy = 0;

                for (int i = 0; i < N; i++) {
                    double delta = trellis[t - 1][i] + MathEx.log(a.get(i, j));

                    if (maxDelta < delta) {
                        maxDelta = delta;
                        maxPsy = i;
                    }
                }

                trellis[t][j] = maxDelta + MathEx.log(b.get(j, o[t]));
                psy[t][j] = maxPsy;
            }
        }

        // trace back
        int n = o.length - 1;
        double maxDelta = Double.NEGATIVE_INFINITY;
        for (int i = 0; i < N; i++) {
            if (maxDelta < trellis[n][i]) {
                maxDelta = trellis[n][i];
                s[n] = i;
            }
        }

        for (int t = n; t-- > 0;) {
            s[t] = psy[t + 1][s[t + 1]];
        }

        return s;
    }

    /**
     * Fits an HMM by maximum likelihood estimation.
     *
     * @param observations the observation sequences, of which symbols take
     * values in [0, n), where n is the number of unique symbols.
     * @param labels the state labels of observations, of which states take
     * values in [0, p), where p is the number of hidden states.
     */
    public static HMM fit(int[][] observations, int[][] labels) {
        if (observations.length != labels.length) {
            throw new IllegalArgumentException("The number of observation sequences and that of label sequences are different.");
        }

        int N = 0; // the number of states
        int M = 0; // the number of symbols

        for (int i = 0; i < observations.length; i++) {
            if (observations[i].length != labels[i].length) {
                throw new IllegalArgumentException(String.format("The length of observation sequence %d and that of corresponding label sequence are different.", i));
            }

            N = Math.max(N, MathEx.max(labels[i]) + 1);
            M = Math.max(M, MathEx.max(observations[i]) + 1);
        }

        double[] pi = new double[N];
        double[][] a = new double[N][N];
        double[][] b = new double[N][M];

        for (int i = 0; i < observations.length; i++) {
            pi[labels[i][0]]++;
            b[labels[i][0]][observations[i][0]]++;
            for (int j = 1; j < observations[i].length; j++) {
                a[labels[i][j - 1]][labels[i][j]]++;
                b[labels[i][j]][observations[i][j]]++;
            }
        }

        MathEx.unitize1(pi);
        for (int i = 0; i < N; i++) {
            MathEx.unitize1(a[i]);
            MathEx.unitize1(b[i]);
        }

        return new HMM(pi, Matrix.of(a), Matrix.of(b));
    }

    /**
     * Fits an HMM by maximum likelihood estimation.
     *
     * @param observations the observation sequences.
     * @param labels the state labels of observations, of which states take
     *               values in [0, p), where p is the number of hidden states.
     * @param ordinal a lambda returning the ordinal numbers of symbols.
     */
    public static <T> HMM fit(T[][] observations, int[][] labels, ToIntFunction<T> ordinal) {
        if (observations.length != labels.length) {
            throw new IllegalArgumentException("The number of observation sequences and that of label sequences are different.");
        }

        return fit(
                Arrays.stream(observations)
                        .map(sequence -> Arrays.stream(sequence).mapToInt(symbol -> ordinal.applyAsInt(symbol)).toArray())
                        .toArray(int[][]::new),
                labels);
    }

    /**
     * Updates the HMM by the Baum-Welch algorithm.
     *
     * @param observations the training observation sequences.
     * @param iterations the number of iterations to execute.
     * @param ordinal a lambda returning the ordinal numbers of symbols.
     */
    public <T> void update(T[][] observations, int iterations, ToIntFunction<T> ordinal) {
        update(
                Arrays.stream(observations)
                        .map(sequence -> Arrays.stream(sequence).mapToInt(symbol -> ordinal.applyAsInt(symbol)).toArray())
                        .toArray(int[][]::new),
                iterations);
    }

    /**
     * Updates the HMM by the Baum-Welch algorithm.
     *
     * @param observations the training observation sequences.
     * @param iterations the number of iterations to execute.
     */
    public void update(int[][] observations, int iterations) {
        for (int iter = 0; iter < iterations; iter++) {
            iterate(observations);
        }
    }

    /**
     * Performs one iteration of the Baum-Welch algorithm.
     *
     * @param sequences the training observation sequences.
     */
    private void iterate(int[][] sequences) {
        int N = a.nrows();
        int M = b.ncols();

        // gamma[n] = gamma array associated to observation sequence n
        double gamma[][][] = new double[sequences.length][][];

        // a[i][j] = aijNum[i][j] / aijDen[i]
        // aijDen[i] = expected number of transitions from state i
        // aijNum[i][j] = expected number of transitions from state i to j
        double aijNum[][] = new double[N][N];
        double aijDen[] = new double[N];

        for (int k = 0; k < sequences.length; k++) {
            if (sequences[k].length <= 2) {
                throw new IllegalArgumentException(String.format("Training sequence %d is too short.", k));
            }

            int[] o = sequences[k];
            double[][] alpha = new double[o.length][N];
            double[][] beta = new double[o.length][N];
            double[] scaling = new double[o.length];
            forward(o, alpha, scaling);
            backward(o, beta, scaling);

            double xi[][][] = estimateXi(o, alpha, beta);
            double g[][] = gamma[k] = estimateGamma(xi);

            int n = o.length - 1;
            for (int i = 0; i < N; i++) {
                for (int t = 0; t < n; t++) {
                    aijDen[i] += g[t][i];

                    for (int j = 0; j < N; j++) {
                        aijNum[i][j] += xi[t][i][j];
                    }
                }
            }
        }

        for (int i = 0; i < N; i++) {
            if (aijDen[i] != 0.0) {
                for (int j = 0; j < N; j++) {
                    a.set(i, j, aijNum[i][j] / aijDen[i]);
                }
            }
        }

        /*
         * initial state probability computation
         */
        Arrays.fill(pi, 0.0);
        for (int j = 0; j < sequences.length; j++) {
            for (int i = 0; i < N; i++) {
                pi[i] += gamma[j][0][i];
            }
        }

        for (int i = 0; i < N; i++) {
            pi[i] /= sequences.length;
        }

        /*
         * emission probability computation
         */
        b.fill(0.0);
        for (int i = 0; i < N; i++) {
            double sum = 0.0;

            for (int j = 0; j < sequences.length; j++) {
                int[] o = sequences[j];
                for (int t = 0; t < o.length; t++) {
                    b.add(i, o[t], gamma[j][t][i]);
                    sum += gamma[j][t][i];
                }
            }

            for (int j = 0; j < M; j++) {
                b.div(i, j, sum);
            }
        }
    }

    /**
     * Here, the xi (and, thus, gamma) values are not divided by the probability
     * of the sequence because this probability might be too small and induce an
     * underflow. xi[t][i][j] still can be interpreted as P(q_t = i and q_(t+1)
     * = j | O, HMM) because we assume that the scaling factors are such that
     * their product is equal to the inverse of the probability of the sequence.
     */
    private double[][][] estimateXi(int[] o, double[][] alpha, double[][] beta) {
        if (o.length <= 1) {
            throw new IllegalArgumentException("Observation sequence is too short.");
        }

        int N = a.nrows();
        int n = o.length - 1;
        double xi[][][] = new double[n][N][N];

        for (int t = 0; t < n; t++) {
            for (int i = 0; i < N; i++) {
                for (int j = 0; j < N; j++) {
                    xi[t][i][j] = alpha[t][i] * a.get(i, j) * b.get(j, o[t + 1]) * beta[t + 1][j];
                }
            }
        }

        return xi;
    }

    /**
     * gamma[][] could be computed directly using the alpha and beta arrays, but
     * this (slower) method is preferred because it doesn't change if the xi
     * array has been scaled (and should be changed with the scaled alpha and
     * beta arrays).
     */
    private double[][] estimateGamma(double[][][] xi) {
        int N = a.nrows();
        double[][] gamma = new double[xi.length + 1][N];

        for (int t = 0; t < xi.length; t++) {
            for (int i = 0; i < N; i++) {
                for (int j = 0; j < N; j++) {
                    gamma[t][i] += xi[t][i][j];
                }
            }
        }

        int n = xi.length - 1;
        for (int j = 0; j < N; j++) {
            for (int i = 0; i < N; i++) {
                gamma[xi.length][j] += xi[n][i][j];
            }
        }

        return gamma;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append(String.format("HMM (%d states, %d emission symbols)%n", a.nrows(), b.ncols()));

        sb.append("Initial state probability: ");
        sb.append(Strings.toString(pi));

        sb.append("\nState transition probability:\n");
        sb.append(a.toString());

        sb.append("Symbol emission probability:\n");
        sb.append(b.toString());

        return sb.toString();
    }
}

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

package smile.data.formula;

import java.io.Serializable;
import java.util.*;
import java.util.stream.Collectors;

import smile.data.DataFrame;
import smile.data.Tuple;
import smile.data.type.*;
import smile.data.vector.*;
import smile.math.matrix.DenseMatrix;
import smile.math.matrix.Matrix;

/**
 * A formula extracts the variables from a context
 * (e.g. Java Class or DataFrame).
 *
 * @author Haifeng Li
 */
public class Formula implements Serializable {
    /** The left-hand side of formula. */
    private Optional<Term> response = Optional.empty();
    /** The right-hand side of formula. */
    private HyperTerm[] predictors;
    /** The terms (both predictors and response) after binding to a schema and expanding the hyper-terms. */
    private Term[] terms;
    /** The terms (only predictors) after binding to a schema and expanding the hyper-terms. */
    private Term[] rhs;
    /** The formula output schema. */
    private StructType schema;

    /**
     * Constructor.
     * @param response the response formula, i.e. dependent variable.
     */
    public Formula(String response) {
        this(new Variable(response));
    }

    /**
     * Constructor.
     * @param response the response formula, i.e. dependent variable.
     */
    public Formula(Term response) {
        this.response = Optional.of(response);
        this.predictors = new HyperTerm[] { new All() };
    }

    /**
     * Constructor.
     * @param predictors the right-hand side of formula, i.e. independent/predictor variables.
     */
    public Formula(HyperTerm[] predictors) {
        this.predictors = predictors;
    }

    /**
     * Constructor.
     * @param response the left-hand side of formula, i.e. dependent variable.
     * @param predictors the right-hand side of formula, i.e. independent/predictor variables.
     */
    public Formula(String response, HyperTerm[] predictors) {
        this(new Variable(response), predictors);
    }

    /**
     * Constructor.
     * @param response the left-hand side of formula, i.e. dependent variable.
     * @param predictors the right-hand side of formula, i.e. independent/predictor variables.
     */
    public Formula(Term response, HyperTerm[] predictors) {
        this.response = Optional.of(response);
        this.predictors = predictors;
    }

    @Override
    public String toString() {
        return String.format("%s ~ %s",
                response.map(Objects::toString).orElse(""),
                Arrays.stream(predictors).map(Objects::toString).collect(Collectors.joining(" + ")));
    }

    /**
     * Factory method.
     * @param lhs the left-hand side of formula, i.e. dependent variable.
     */
    public static Formula lhs(String lhs) {
        return new Formula(lhs);
    }

    /**
     * Factory method.
     * @param lhs the left-hand side of formula, i.e. dependent variable.
     */
    public static Formula lhs(Term lhs) {
        return new Formula(lhs);
    }

    /**
     * Factory method.
     * @param predictors the right-hand side of formula, i.e. independent/predictor variables.
     */
    public static Formula rhs(HyperTerm... predictors) {
        return new Formula(predictors);
    }

    /**
     * Factory method.
     * @param response the left-hand side of formula, i.e. dependent variable.
     * @param predictors the right-hand side of formula, i.e. independent/predictor variables.
     */
    public static Formula of(String response, HyperTerm... predictors) {
        return new Formula(response, predictors);
    }

    /**
     * Factory method.
     * @param response the left-hand side of formula, i.e. dependent variable.
     * @param predictors the right-hand side of formula, i.e. independent/predictor variables.
     */
    public static Formula of(Term response, HyperTerm... predictors) {
        return new Formula(response, predictors);
    }

    /** Returns the terms of formula. This should be called after bind() called. */
    public Term[] terms() {
        return terms;
    }

    /**
     * Apply the formula on a tuple to generate the model data.
     */
    public Tuple apply(Tuple t) {
        return new smile.data.AbstractTuple() {
            @Override
            public StructType schema() {
                return schema;
            }

            @Override
            public Object get(int i) {
                return terms[i].apply(t);
            }

            @Override
            public int getInt(int i) {
                return terms[i].applyAsInt(t);
            }

            @Override
            public long getLong(int i) {
                return terms[i].applyAsLong(t);
            }

            @Override
            public float getFloat(int i) {
                return terms[i].applyAsFloat(t);
            }

            @Override
            public double getDouble(int i) {
                return terms[i].applyAsDouble(t);
            }

            @Override
            public String toString() {
                return schema.toString(this);
            }
        };
    }

    /** Binds the formula to a schema and returns the output schema of formula. */
    public StructType bind(StructType inputSchema) {
        if (schema != null) {
            return schema;
        }

        Arrays.stream(predictors).forEach(term -> term.bind(inputSchema));

        List<Term> factors = Arrays.stream(predictors)
                .filter(term -> !(term instanceof All) && !(term instanceof Delete))
                .flatMap(term -> term.terms().stream())
                .collect(Collectors.toList());

        response.ifPresent(r -> {
            r.bind(inputSchema);
            factors.add(r);
        });

        List<Term> removes = Arrays.stream(predictors)
                .filter(term -> term instanceof Delete)
                .flatMap(term -> term.terms().stream())
                .collect(Collectors.toList());

        Set<String> variables = factors.stream()
            .flatMap(factor -> factor.variables().stream())
            .collect(Collectors.toSet());

        Optional<All> hasAll = Arrays.stream(predictors)
                .filter(term -> term instanceof All)
                .map(term -> (All) term)
                .findAny();

        List<Term> result = new ArrayList<>();
        hasAll.ifPresent(all -> {
            java.util.stream.Stream<Variable> stream = all.terms().stream();
            if (all.rest()) {
                stream = stream.filter(term -> !variables.contains(term.name()));
                if (response.isPresent())
                    stream = stream.filter(term -> !term.equals(response.get()));
            }

            result.addAll(stream.collect(Collectors.toList()));
        });

        result.addAll(factors);
        result.removeAll(removes);
        this.terms = result.toArray(new Term[result.size()]);
        if (response.isPresent()) {
            this.rhs = Arrays.stream(this.terms).filter(term -> term != response.get()).toArray(Term[]::new);
        } else {
            this.rhs = terms;
        }

        List<StructField> fields = result.stream()
                .map(factor -> new StructField(factor.toString(), factor.type(), factor.measure()))
                .collect(Collectors.toList());

        schema = DataTypes.struct(fields);
        return schema;
    }

    /**
     * Returns a data frame with the variables needed to use formula.
     * @param df The input DataFrame.
     */
    public DataFrame frame(DataFrame df) {
        bind(df.schema());
        BaseVector[] vectors = Arrays.stream(terms()).map(term -> term.apply(df)).toArray(BaseVector[]::new);
        return DataFrame.of(vectors);
    }

    /**
     * Creates a design (or model) matrix without bias column.
     * @param df The input DataFrame.
     */
    public DenseMatrix matrix(DataFrame df) {
        return matrix(df, false);
    }

    /**
     * Creates a design (or model) matrix.
     * @param df The input DataFrame.
     * @param bias If true, the design matrix includes an all-one column for intercept.
     */
    public DenseMatrix matrix(DataFrame df, boolean bias) {
        bind(df.schema());
        Term[] terms = terms();

        int nrows = df.nrows();
        int ncols = terms.length;

        if (response.isPresent()) {
            ncols -= 1;
        }

        ncols += bias ? 1 : 0;
        DenseMatrix m = Matrix.of(nrows, ncols, 0.0);
        if (bias) for (int i = 0; i < nrows; i++) m.set(i, ncols-1, 1.0);

        for (int j = 0, jj = 0; jj < terms.length; j++, jj++) {
            if (response.isPresent() && terms[jj] == response.get()) continue;

            BaseVector v = terms[jj].apply(df);
            DataType type = terms[jj].type();
            switch (type.id()) {
                case Double:
                case Integer:
                case Float:
                case Long:
                case Boolean:
                case Byte:
                case Short:
                case Char: {
                    for (int i = 0; i < nrows; i++) m.set(i, j, v.getDouble(i));
                    break;
                }

                case String: {
                    for (int i = 0; i < nrows; i++) {
                        String s = (String) v.get(i);
                        m.set(i, j, s == null ? Double.NaN : Double.valueOf(s));
                    }
                    break;
                }

                case Object: {
                    Class clazz = ((ObjectType) type).getObjectClass();
                    if (clazz == Boolean.class) {
                        for (int i = 0; i < nrows; i++) {
                            Boolean b = (Boolean) v.get(i);
                            if (b != null)
                                m.set(i, j, b.booleanValue() ? 1 : 0);
                            else
                                m.set(i, j, Double.NaN);
                        }
                    } else if (Number.class.isAssignableFrom(clazz)) {
                        for (int i = 0; i < nrows; i++) m.set(i, j, v.getDouble(i));
                    } else {
                        throw new UnsupportedOperationException(String.format("DataFrame.toMatrix() doesn't support type %s", type));
                    }
                    break;
                }

                default:
                    throw new UnsupportedOperationException(String.format("DataFrame.toMatrix() doesn't support type %s", type));
            }
        }

        return m;
    }

    /**
     * Returns the response vector.
     * @param df The input DataFrame.
     */
    public BaseVector response(DataFrame df) {
        return response.map(r -> {
            r.bind(df.schema());
            return r.apply(df);
        }).orElse(null);
    }

    /**
     * Returns the real-valued response value.
     */
    public double response(Tuple t) {
        return response.map(r -> r.applyAsDouble(t)).orElse(0.0);
    }

    /**
     * Returns the class label.
     */
    public int label(Tuple t) {
        return response.map(r -> r.applyAsInt(t)).orElse(-1);
    }

    /**
     * Returns the names of response variable.
     */
    public Optional<String> response() {
        return response.map(Objects::toString);
    }

    /**
     * Returns the real values of predictors.
     */
    public double[] predictors(Tuple t) {
        return Arrays.stream(rhs).mapToDouble(term -> term.applyAsDouble(t)).toArray();
    }

    /**
     * Returns the names of predictors.
     */
    public String[] predictors() {
        return Arrays.stream(rhs).map(Object::toString).toArray(String[]::new);
    }

    /**
     * Returns the schema of output data frame.
     */
    public StructType schema() {
        return schema;
    }

    /**
     * Returns the schema of design matrix.
     */
    public StructType predictorSchema() {
        Optional<String> y = response();
        if (y.isPresent()) {
            StructField[] fields = Arrays.stream(schema.fields()).filter(field -> !field.name.equals(y.get())).toArray(StructField[]::new);
            return DataTypes.struct(fields);
        } else {
            return schema;
        }
    }
}

package jads.mp;

import java.util.*;

/**
 * This class represents a Linear Expression, used mainly to express Linear
 * Constraints.
 *
 * @author Tulio Toffolo
 */
public class MPLinExpr {

    private double constant = 0.;
    private Map<MPVar, Double> coeffs = new LinkedHashMap<MPVar, Double>();


    /**
     * Instantiates a new Linear Expression.
     */
    public MPLinExpr() { }

    /**
     * Instantiates a new Linear Expression.
     *
     * @param constant a constant value in the expression
     */
    public MPLinExpr(double constant) {
        this.constant = constant;
    }

    /**
     * Instantiates a new Linear Expression with a variable.
     *
     * @param variable a variable in the expression
     */
    public MPLinExpr(MPVar variable) {
        setCoeff(variable, 1.);
    }

    /**
     * Instantiates a new Linear Expression with a variable (and its
     * coefficient).
     *
     * @param coeff    the coefficient of the variable
     * @param variable the variable in the expression
     */
    public MPLinExpr(double coeff, MPVar variable) {
        setCoeff(variable, coeff);
    }

    /**
     * Instantiates a new Linear Expression with a variable (and its
     * coefficient).
     *
     * @param variable the variable in the expression
     * @param coeff    the coefficient of the variable
     */
    public MPLinExpr(MPVar variable, double coeff) {
        setCoeff(variable, coeff);
    }

    /**
     * Instantiates a new Linear Expression with an array of variables (and
     * their coefficients).
     *
     * @param coeffs    the coefficients of the variables
     * @param variables the array of variables
     */
    public MPLinExpr(double coeffs[], MPVar variables[]) {
        assert variables.length == coeffs.length;

        for (int i = 0; i < variables.length; i++) {
            setCoeff(variables[i], coeffs[i]);
        }
    }

    /**
     * Instantiates a new Linear Expression with an array of variables (and
     * their coefficients).
     *
     * @param variables the array of variables
     * @param coeffs    the coefficients of the variables
     */
    public MPLinExpr(MPVar variables[], double coeffs[]) {
        assert variables.length == coeffs.length;

        for (int i = 0; i < variables.length; i++) {
            setCoeff(variables[i], coeffs[i]);
        }
    }

    /**
     * * Instantiates a new Linear Expression from an existing Linear
     * Expression.
     *
     * @param linearExpr the Linear Expression to be copied
     */
    public MPLinExpr(MPLinExpr linearExpr) {
        this.constant = linearExpr.constant;
        this.coeffs.putAll(linearExpr.coeffs);
    }

    /**
     * * Instantiates a new Linear Expression from an existing Linear
     * Expression.
     *
     * @param linearExpr the Linear Expression to be copied
     * @param constant   constant to add to the Linear Expression
     */
    public MPLinExpr(MPLinExpr linearExpr, double constant) {
        this.constant = linearExpr.constant + constant;
        this.coeffs.putAll(linearExpr.coeffs);
    }


    /**
     * Gets an iterable of the non-zero variables (and their respectively
     * coefficients) in this Linear Expression.
     *
     * @return the iterable of the non-zero variables (and their respectively
     * coefficients) in this Linear Expression.
     */
    public Iterable<Map.Entry<MPVar, Double>> coeffs() {
        return coeffs.entrySet();
    }

    /**
     * Adds a constant to the Linear Expression.
     *
     * @param constant the constant
     * @return this (updated) Linear Expression
     */
    public MPLinExpr addConstant(double constant) {
        this.constant += constant;
        return this;
    }

    /**
     * Adds a Linear Expression to this Linear Expression.
     *
     * @param linearExpr the Linear Expression to be added
     * @param multiplier the multiplier for all coefficients of the Linear
     *                   Expression to be added
     * @return this (updated) Linear Expression
     */
    public MPLinExpr addExpr(MPLinExpr linearExpr, double multiplier) {
        if (Math.abs(multiplier) < MPModel.EPS) return this;

        for (Map.Entry<MPVar, Double> entry : linearExpr.coeffs())
            addTerm(entry.getKey(), multiplier * entry.getValue());
        addConstant(multiplier * linearExpr.getConstant());
        return this;
    }

    /**
     * Adds a term (variable and coefficient) to this Linear Expression.
     *
     * @param variable the variable to be added
     * @param coeff    the coefficient of the variable
     * @return this (updated) Linear Expression
     */
    public MPLinExpr addTerm(MPVar variable, double coeff) {
        Double currentCoeff = coeffs.get(variable);
        if (currentCoeff != null)
            coeff += currentCoeff;

        setCoeff(variable, coeff);
        return this;
    }


    // region mirror methods for addExpr and addTerm

    /**
     * Adds a term (variable and coefficient) to this Linear Expression.
     *
     * @param coeff    the coefficient of the variable
     * @param variable the variable to be added
     * @return this (updated) Linear Expression
     */
    public MPLinExpr addTerm(double coeff, MPVar variable) {
        return addTerm(variable, coeff);
    }

    /**
     * Adds a Linear Expression to this Linear Expression.
     *
     * @param multiplier the multiplier for all coefficients of the Linear
     *                   Expression to be added
     * @param linearExpr the Linear Expression to be added
     * @return this (updated) Linear Expression
     */
    public MPLinExpr addExpr(double multiplier, MPLinExpr linearExpr) {
        return addExpr(linearExpr, multiplier);
    }

    // endregion mirror methods for addExpr and addTerm


    /**
     * Gets the coefficient of a variable.
     *
     * @param variable the variable
     * @return the coefficient of the variable passed as argument
     */
    public double getCoeff(MPVar variable) {
        Double coeff = coeffs.get(variable);
        if (coeff != null)
            return coeff;
        return 0.;
    }

    /**
     * Gets the constant value of this Linear Expression.
     *
     * @return the constant value of this Linear Expression
     */
    public double getConstant() {
        return constant;
    }

    /**
     * Returns true if all coefficients in this Linear Expression are zero and
     * false otherwise.
     *
     * @return true if all coefficients in this Linear Expression are zero and
     * false otherwise.
     */
    public boolean isEmpty() {
        return coeffs.isEmpty();
    }

    /**
     * Sets the constant of this Linear Expression.
     *
     * @param constant the constant
     */
    public void setConstant(double constant) {
        this.constant = constant;
    }

    /**
     * Gets if a variable has non-zero coefficient in this Linear Expression.
     *
     * @param variable the variable
     * @return true if the coefficient of the variable in this Linear Expression
     * is not zero, and false otherwise.
     */
    public boolean hasVar(MPVar variable) {
        return coeffs.containsKey(variable);
    }

    /**
     * Removes a variable from this Linear Expression, i.e. sets its coefficient
     * to zero.
     *
     * @param variable the variable
     */
    public void removeVar(MPVar variable) {
        setCoeff(variable, 0.);
    }

    /**
     * Sets the coefficient of the variable passed as argument in this Linear
     * Expression.
     *
     * @param variable the variable
     * @param value    the new coefficient for the variable
     */
    public void setCoeff(MPVar variable, double value) {
        assert variable != null : "null variables are invalid keys";

        if (Math.abs(value) < MPModel.EPS && coeffs.containsKey(variable))
            coeffs.remove(variable);
        else if (Math.abs(value) >= MPModel.EPS)
            coeffs.put(variable, value);
    }


    /**
     * Sets all coefficients of this Linear Expression to zero.
     */
    protected void clear() {
        constant = 0.;
        coeffs.clear();
    }
}

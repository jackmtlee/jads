package jads.mp;

import java.util.*;

/**
 * This class represents a binary, continuous or integer Variable.
 *
 * @author Tulio Toffolo
 */
public class MPVar implements Comparable<MPVar> {

    public static final char BINARY = 'B', CONTINUOUS = 'C', INTEGER = 'I';

    public final MPModel model;

    private final String name;

    protected int index = -1;

    private double lb = 0.0;
    private double ub = Double.MAX_VALUE;
    private char type = 'C';
    private MPColumn column = new MPColumn();


    /**
     * Instantiates a new Variable.
     *
     * @param model the model to which the variable will be added
     * @param lb    the lower bound of the variable
     * @param ub    the upper bound of the variable
     * @param type  the type of the variable, i.e. Variable.BINARY ('B'),
     *              Variable.CONTINUOUS ('C') or Variable.INTEGER ('I')
     * @param name  the name of the variable
     */
    protected MPVar(MPModel model, double lb, double ub, char type, String name) {
        this.model = model;
        this.lb = lb;
        this.ub = ub;
        this.type = type;
        this.name = name;

        assert type != 'B' || lb >= 0. && ub <= 1.;
    }


    /**
     * Gets an iterable over the coefficients of this variable in a set of
     * Linear Constraints.
     *
     * @return the iterable
     */
    public Iterable<Map.Entry<MPLinConstr, Double>> coeffs() {
        return column.entrySet();
    }

    @Override
    public int compareTo(MPVar variable) {
        return Integer.compare(index, variable.index);
    }

    /**
     * Gets the coefficient of this variable in a constraint.
     *
     * @param constr the constraint
     * @return the coefficient of this variable in the constraint given as
     * argument
     */
    public double getCoeff(MPLinConstr constr) {
        Double coeff = column.get(constr);
        if (coeff == null)
            return 0;
        return coeff;
    }

    /**
     * Gets the index of this variable.
     *
     * @return the index of this variable
     */
    public int getIndex() {
        return index;
    }

    /**
     * Gets the coefficient of this variable in the Objective.
     *
     * @return the coefficient of this variable in the Objective
     */
    public double getObj() {
        return column.obj;
    }

    /**
     * Get if this variable is integer (or binary).
     *
     * @return true if the variable is integer (or binary) and false otherwise
     */
    public boolean isInteger() {
        return type == 'B' || type == 'I';
    }

    /**
     * Gets the lower bound of this variable.
     *
     * @return the lower bound value
     */
    public double getLB() {
        return lb;
    }

    /**
     * Gets the name of this variable
     *
     * @return the name of this variable
     */
    public String getName() {
        return name;
    }

    /**
     * Gets the number of constraints in which this variable has a non-zero
     * coefficient.
     *
     * @return the number of constraints in which this variable has a non-zero
     * coefficient.
     */
    public int getNConstrs() {
        return column.size();
    }

    /**
     * Gets the type of this variable, i.e. Variable.BINARY ('B'),
     * Variable.CONTINUOUS ('C') or Variable.INTEGER ('I').
     *
     * @return the type of this variable
     */
    public char getType() {
        return type;
    }

    /**
     * Gets the upper bound of this variable.
     *
     * @return the upper bound value
     */
    public double getUB() {
        return ub;
    }

    @Override
    public int hashCode() {
        return name.hashCode();
    }

    /**
     * Sets the lower and upper bounds of a variable already in the model.
     *
     * @param ub the new upper bound value.
     * @param lb the new lower bound value.
     */
    public void setBounds(double lb, double ub) {
        this.lb = lb;
        this.ub = ub;
        model.addAction(new MPAction(MPAction.ActionType.SetVarBounds, this));
    }

    /**
     * Sets the lower bound of this variable.
     *
     * @param lb the new lower bound of this variable
     */
    public void setLB(double lb) {
        this.lb = lb;
        model.addAction(new MPAction(MPAction.ActionType.SetVarBounds, this));
    }

    /**
     * Sets the upper bound of this variable.
     *
     * @param ub the new lower bound of this variable
     */
    public void setUB(double ub) {
        this.ub = ub;
        model.addAction(new MPAction(MPAction.ActionType.SetVarBounds, this));
    }

    /**
     * Sets the type of this variable
     *
     * @param type the new type for this variable ('B', 'C' or 'I')
     */
    public void setType(char type) {
        if (this.type != type) {
            if (this.type == CONTINUOUS && (type == BINARY || type == INTEGER))
                model.intVarCount++;

            this.type = Character.toUpperCase(type);
            model.addAction(new MPAction(MPAction.ActionType.SetVarType, this));
        }
    }


    /**
     * Deletes this variable from all rows (constraints).
     */
    protected MPColumn getColumn() {
        return column;
    }

    /**
     * Sets the coefficient of this variable in a constraint.
     *
     * @param constr the constraint
     * @param value  the coefficient value
     */
    protected void setCoeff(MPLinConstr constr, Double value) {
        if (Math.abs(value) < MPModel.EPS && column.containsKey(constr))
            column.remove(constr);
        else if (Math.abs(value) >= MPModel.EPS)
            column.put(constr, value);
    }

    /**
     * Sets the coefficient of this variable in the Objective.
     *
     * @param obj the coefficient value
     */
    protected void setObj(double obj) {
        column.obj = obj;
    }
}

package jads.mp;

import java.util.*;

/**
 * This class represents a Linear Constraint.
 *
 * @author Tulio Toffolo
 */
public class MPLinConstr extends MPLinExpr implements Comparable<MPLinConstr>, MPConstr {

    public static final char LE = '<', EQ = '=', GE = '>';

    private final String name;
    protected int index = -1;
    private char sense = '?';


    /**
     * Instantiates a new Linear Constraint.
     *
     * @param model the model to add the constraint to
     * @param index the index of the constraint
     * @param lhs   the expression of the left-hand size of the constraint
     * @param sense the sense of the constraint ('&lt;', '&gt;' or '=')
     * @param name  the name of the constraint
     */
    protected MPLinConstr(MPModel model, int index, MPLinExpr lhs, char sense, String name) {
        super(lhs);
        assert sense == LE || sense == EQ || sense == GE : "invalid sense for constraint " + name;

        //this.model = model;
        this.index = index;
        this.sense = sense;
        this.name = name;

        for (Map.Entry<MPVar, Double> entry : coeffs())
            entry.getKey().setCoeff(this, entry.getValue());
    }

    /**
     * Instantiates a new Linear Constraint.
     *
     * @param model the model to add the constraint to
     * @param index the index of the constraint
     * @param lhs   the expression of the left-hand size of the constraint
     * @param sense the sense of the constraint ('&lt;', '&gt;' or '=')
     * @param rhs   the expression of the right-hand size of the constraint
     * @param name  the name of the constraint
     */
    protected MPLinConstr(MPModel model, int index, MPLinExpr lhs, char sense, MPLinExpr rhs, String name) {
        super(lhs);
        addExpr(rhs, -1);
        assert sense == LE || sense == EQ || sense == GE : "invalid sense for constraint " + name;

        //this.model = model;
        this.index = index;
        this.sense = sense;
        this.name = name;

        for (Map.Entry<MPVar, Double> entry : coeffs())
            entry.getKey().setCoeff(this, entry.getValue());
    }


    @Override
    public int getIndex() {
        return index;
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public char getSense() {
        return sense;
    }

    @Override
    public int compareTo(MPLinConstr constr) {
        return Integer.compare(index, constr.index);
    }

    @Override
    public int hashCode() {
        return name.hashCode();
    }

    @Override
    public void setCoeff(MPVar variable, double coeff) {
        super.setCoeff(variable, coeff);
        variable.setCoeff(this, coeff);
    }
}

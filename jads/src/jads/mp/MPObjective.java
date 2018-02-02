package jads.mp;

import java.util.*;

/**
 * This class represents a Linear Objective.
 *
 * @author Tulio Toffolo
 */
public class MPObjective extends MPLinExpr {

    public static final char MINIMIZE = '-', MAXIMIZE = '+';

    public final String name;
    private final int hashCode;
    protected char direction = MINIMIZE;


    /**
     * Instantiates a new Linear Objective.
     */
    protected MPObjective() {
        super(0.);
        this.name = "objective";
        this.hashCode = name.hashCode();
    }


    @Override
    public void clear() {
        for (Map.Entry<MPVar, Double> entry : coeffs())
            entry.getKey().setObj(0.);
        super.clear();
    }

    /**
     * Gets the direction of the optimization, i.e. Objective.MINIMIZE ('-') or
     * Objective.MAXIMIZE ('+').
     *
     * @return the direction of the optimization
     */
    public char getDirection() {
        return direction;
    }

    /**
     * Gets the name of the Objective.
     *
     * @return the name of the Objective
     */
    public String getName() {
        return name;
    }

    @Override
    public int hashCode() {
        return hashCode;
    }

    /**
     * Sets the direction of the optimization, i.e. Objective.MINIMIZE ('-') or
     * Objective.MAXIMIZE ('+').
     *
     * @param direction the direction, Objective.MINIMIZE or Objective.MAXIMIZE.
     */
    public void setDirection(char direction) {
        this.direction = direction;
    }

    @Override
    public void setCoeff(MPVar variable, double coeff) {
        super.setCoeff(variable, coeff);
        variable.setObj(coeff);
    }
}

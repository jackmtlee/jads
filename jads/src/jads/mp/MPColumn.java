package jads.mp;

import java.util.*;

/**
 * This class represents a Column.
 *
 * @author Tulio Toffolo
 */
public class MPColumn extends LinkedHashMap<MPLinConstr, Double> implements Iterable<Map.Entry<MPLinConstr, Double>> {

    protected double obj = 0.0;

    public MPColumn() {
        super();
    }

    public MPColumn(double obj) {
        super();
        this.obj = obj;
    }

    /**
     * Adds a coefficient in a contraints to the column.
     *
     * @param coeff  the coefficient value
     * @param constr the constraint
     */
    public void add(double coeff, MPLinConstr constr) {
        put(constr, coeff);
    }

    /**
     * Adds several coefficients (of constraints) to the column.
     *
     * @param constrs the constraints
     * @param coeffs  the coefficients
     */
    public void add(MPLinConstr constrs[], double coeffs[]) {
        assert constrs.length == coeffs.length;

        for (int i = 0; i < constrs.length; i++)
            put(constrs[i], coeffs[i]);
    }

    // region mirror methods for add

    /**
     * Adds several coefficients (of constraints) to the column.
     *
     * @param coeffs  the coefficients
     * @param constrs the constraints
     */
    public void add(double coeffs[], MPLinConstr constrs[]) {
        add(constrs, coeffs);
    }

    /**
     * Adds several coefficients (of constraints) to the column.
     *
     * @param constr the constraint
     * @param coeff  the coefficient
     */
    public void add(MPLinConstr constr, double coeff) {
        put(constr, coeff);
    }

    // endregion mirror methods for add

    @Override
    public Iterator<Map.Entry<MPLinConstr, Double>> iterator() {
        return entrySet().iterator();
    }
}

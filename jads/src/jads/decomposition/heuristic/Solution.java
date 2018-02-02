package jads.decomposition.heuristic;

import jads.mp.*;

import java.util.*;

/**
 * This class represents a Solution (or partial Solution).
 *
 * @author Tulio Toffolo
 */
public class Solution {

    public final MPModel model;

    private boolean hasValue[];
    private double x[];

    public Solution(MPModel model) {
        this.model = model;
        hasValue = new boolean[model.getNVars()];
        x = new double[model.getNVars()];
    }

    public Solution(Solution solution) {
        this.model = solution.model;

        this.hasValue = Arrays.copyOf(solution.hasValue, solution.hasValue.length);
        this.x = Arrays.copyOf(solution.x, solution.x.length);
    }


    public double getObjective() {
        double obj = 0;
        for (int i = 0; i < x.length; i++) {
            obj += x[i] * model.getVar(i).getObj();
        }
        return obj;
    }

    public Double getValue(String name) {
        MPVar var = model.getVar(name);
        return var == null ? null : !hasValue[var.getIndex()] ? null : x[var.getIndex()];
    }

    public Double getValue(int index) {
        return !hasValue[index] ? null : x[index];
    }

    public double[] getX() {
        return x;
    }

    public boolean hasValue(int index) {
        return hasValue[index];
    }

    public boolean hasVar(String name) {
        MPVar var = model.getVar(name);
        return var != null && hasValue[var.getIndex()];
    }

    public void setValue(MPVar var, double value) {
        x[var.getIndex()] = var.isInteger() ? Math.round(value) : value;
    }

    public boolean setValue(String varName, double value) {
        MPVar var = model.getVar(varName);
        if (var == null) {
            System.out.println("Variable " + varName + " not found...");
            return false;
        }
        x[var.getIndex()] = var.isInteger() ? Math.round(value) : value;
        return true;
    }

    public void update(double[] solution) {
        for (int i = 0; i < x.length; i++) {
            hasValue[i] = true;
            x[i] = solution[i];
        }
    }

    public void update(List<MPVar> vars, double[] values) {
        for (int i = 0; i < vars.size(); i++) {
            MPVar var = vars.get(i);
            if (var != null) {
                x[var.getIndex()] = var.isInteger() ? Math.round(values[i]) : values[i];
                hasValue[var.getIndex()] = true;
            }
        }
    }
}

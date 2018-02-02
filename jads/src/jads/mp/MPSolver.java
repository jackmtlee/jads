package jads.mp;

import java.util.*;

/**
 * This class represents a Solver, used to solve the formulation.
 *
 * @author Tulio Toffolo
 */
public abstract class MPSolver {

    protected MPModel input;

    protected double bestBound, rootBound, objValue;
    protected double[] solution;

    protected double[] duals = null;

    protected ArrayList<Double> objValues = new ArrayList<Double>();
    protected ArrayList<double[]> solutions = new ArrayList<double[]>();


    /**
     * Instantiates a new Solver.
     *
     * @param input the input
     */
    protected MPSolver(MPModel input) {
        this.input = input;
    }


    /**
     * Solves the formulation.
     *
     * @return true in case of success and false otherwise
     */
    public boolean solve() {
        return solve(false);
    }

    /**
     * Gets the best bound obtained (obj. value if optimal).
     *
     * @return the best bound obtained (obj. value if optimal)
     */
    public double getBestBound() {
        return bestBound;
    }

    /**
     * Gets the linear relaxation bound.
     *
     * @return the linear relaxation bound
     */
    public double getRootBound() {
        return rootBound;
    }

    /**
     * Gets the objective value.
     *
     * @return the objective value
     */
    public final double getObjValue() { return objValue; }

    /**
     * Gets the objective value of the solution *index*.
     *
     * @param index index of the solution
     * @return the objective value
     */
    public final double getObjValue(int index) { return objValues.get(index); }

    /**
     * Gets the best solution as an array of double values.
     *
     * @return the solution
     */
    public final double[] getSolution() { return solution; }

    /**
     * Gets the objective values of all generated solutions.
     *
     * @return an {@link ArrayList} with the objective values
     */
    public final ArrayList<Double> getObjValues() { return objValues; }

    /**
     * Gets all generated solutions,  each expressed as an array of double
     * values.
     *
     * @return an {@link ArrayList} with all the solutions, each expressed as an
     * array of double values
     */
    public final ArrayList<double[]> getSolutions() { return solutions; }


    /**
     * Gets the dual values (usually referred to as "Pi") of all constraints.
     * Note: if the dual values are not available, then null is returned.
     *
     * @return an array with all dual values of all constraints; if the dual
     * values are not available, then null is returned.
     */
    public final double[] getDuals() {
        return duals;
    }

    /**
     * Gets the dual values (usually referred to as "Pi") of the constraints
     * passed as argument. Note: if the dual values are not available, then null
     * is returned.
     *
     * @param constrs array with constraints to get the dual values for
     * @return an array with the dual values of the constraints passed as
     * argument; if the dual values are not available, then null is returned.
     */
    public final double[] getDuals(MPConstr constrs[]) {
        if (duals == null)
            return null;

        double selectedDuals[] = new double[constrs.length];
        for (int i = 0; i < constrs.length; i++)
            selectedDuals[i] = duals[constrs[i].getIndex()];

        return selectedDuals;
    }


    /**
     * Gets the value of the variable in the current solution.
     *
     * @param variable the variable to get the value of.
     * @return the value of the variable in the current solution.
     */
    public final double getValue(MPVar variable) {
        assert solution != null;
        return solution[variable.getIndex()];
    }

    /**
     * Gets the value of the variable in the index-th solution.
     *
     * @param variable the variable to get the value of.
     * @param index    index of the solution to get the value from.
     * @return the value of the variable in the current solution.
     */
    public final double getValue(MPVar variable, int index) {
        assert solutions.size() > index;
        return solutions.get(index)[variable.getIndex()];
    }


    /**
     * Gets the reduced cost of the variable in the current solution.
     *
     * @param variable the variable to get the value of.
     * @return the value of the variable in the current solution.
     */
    public double getReducedCost(MPVar variable) {
        throw new Error("Not implemented function!");
    }


    /**
     * Adds an initial solution to warm start the solver.
     *
     * @param solution the initial solution (array of variable values)
     */
    public abstract void addSolution(double solution[]);
    /**
     * Adds an initial solution to warm start the solver.
     *
     * @param variables the variables to set
     * @param solution  the initial solution (variable values)
     */
    public abstract void addSolution(MPVar variables[], double solution[]);

    /**
     * Solves the formulation using the populate function (when available).
     *
     * @return true in case of success and false otherwise.
     */
    public abstract boolean populate();
    /**
     * Solves the formulation. Important: if you need the dual values to be
     * calculated, then consider setting linearRelaxation=true; otherwise, the
     * dual values will not be computed.
     *
     * @param linearRelaxation boolean to indicate if the relaxation of the
     *                         formulation should be solved (true) or the
     *                         original formulation. Important: if you need the
     *                         dual values to be calculated, then consider
     *                         setting linearRelaxation=true; otherwise, the
     *                         dual values will not be computed
     * @return true in case of success and false otherwise.
     */
    public abstract boolean solve(boolean linearRelaxation);

    /**
     * Sets the priorities of the variables. The higher the value of the
     * priority of a variable, the higher the chances of it being selecting for
     * branching.
     *
     * @param priorities the priorities
     */
    public abstract void setPriorities(int priorities[]);
    /**
     * Sets the priorities of the variables. The higher the value of the
     * priority of a variable, the higher the chances of it being selecting for
     * branching.
     *
     * @param variables  the variables whose priority are to be set
     * @param priorities the priorities
     */
    public abstract void setPriorities(MPVar variables[], int priorities[]);

    /**
     * Updates the formulation within the solver.
     */
    protected abstract void updateModel();
    /**
     * Updates the objective function.
     */
    protected abstract void updateObjective();

    /**
     * Writes the formulation to a file.
     *
     * @param filename the file path
     */
    public abstract void writeModel(String filename);

    /**
     * Gets a boolean parameter.
     *
     * @param param the parameter
     * @return the parameter value
     */
    public abstract boolean getParam(BooleanParam param);
    /**
     * Gets a double parameter.
     *
     * @param param the parameter
     * @return the parameter value
     */
    public abstract double getParam(DoubleParam param);
    /**
     * Gets an integer parameter.
     *
     * @param param the parameter
     * @return the parameter value
     */
    public abstract int getParam(IntParam param);
    /**
     * Gets a long parameter.
     *
     * @param param the parameter
     * @return the parameter value
     */
    public abstract long getParam(LongParam param);
    /**
     * Gets a String parameter.
     *
     * @param param the parameter
     * @return the parameter value
     */
    public abstract String getParam(StringParam param);

    /**
     * Sets a boolean parameter.
     *
     * @param param the parameter
     * @param value the new value for the parameter
     */
    public abstract void setParam(BooleanParam param, boolean value);
    /**
     * Sets a double parameter.
     *
     * @param param the parameter
     * @param value the new value for the parameter
     */
    public abstract void setParam(DoubleParam param, double value);
    /**
     * Sets an integer parameter.
     *
     * @param param the parameter
     * @param value the new value for the parameter
     */
    public abstract void setParam(IntParam param, int value);
    /**
     * Sets a long parameter.
     *
     * @param param the parameter
     * @param value the new value for the parameter
     */
    public abstract void setParam(LongParam param, long value);
    /**
     * Sets a String parameter.
     *
     * @param param the parameter
     * @param value the new value for the parameter
     */
    public abstract void setParam(StringParam param, String value);

    /**
     * Enum with the possible boolean parameters.
     */
    public enum BooleanParam {
        NumericalEmphasis
    }

    /**
     * Enum with the possible double parameters.
     */
    public enum DoubleParam {
        CutUp,
        CutLo,
        Cutoff,
        MIPGap,
        MIPGapAbs,
        ObjDif,
        RelObjDif,
        TimeLimit,
    }

    /**
     * Enum with the possible integer parameters.
     */
    public enum IntParam {
        LogToConsole,
        PopulateLim,
        RootAlg,
        Threads,
    }

    /**
     * Enum with the possible long parameters.
     */
    public enum LongParam {
        IntSolLim,
        IterLimit,
        NodeLimit,
    }

    /**
     * Enum with the possible String parameters.
     */
    public enum StringParam {
        LogFile,
    }

    /**
     * Enum with the possible solution status.
     */
    public enum SolverStatus {
        Optimal, TimeLimit, Unknown, Error, Infeasible
    }
}

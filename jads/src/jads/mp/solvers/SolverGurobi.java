package jads.mp.solvers;

import gurobi.*;
import jads.mp.*;

import java.util.*;

/**
 * This class is a simple wrapper to use the solver Gurobi.
 *
 * @author Tulio Toffolo
 */
public class SolverGurobi extends MPSolver {

    private GRBModel gurobi;
    private GRBLinExpr objective;
    private GRBVar[] vars = null;
    private ArrayList<GRBVar> binVars = new ArrayList<GRBVar>();
    private ArrayList<GRBVar> intVars = new ArrayList<GRBVar>();

    private double reducedCosts[] = null;

    private boolean relaxed = false;
    private boolean varUpdateRequired = false;

    private int populateMin = 1;

    // region Parameters variables
    private HashMap<BooleanParam, GRB.IntParam> booleanParams = new HashMap<BooleanParam, GRB.IntParam>();
    private HashMap<DoubleParam, GRB.DoubleParam> doubleParams = new HashMap<DoubleParam, GRB.DoubleParam>();
    private HashMap<IntParam, GRB.IntParam> intParams = new HashMap<IntParam, GRB.IntParam>();
    private HashMap<LongParam, GRB.IntParam> longParams = new HashMap<LongParam, GRB.IntParam>();
    private HashMap<StringParam, GRB.StringParam> stringParams = new HashMap<StringParam, GRB.StringParam>();
    // endregion Parameters variables


    /**
     * Instantiates a new Gurobi solver.
     *
     * @param input the input model
     */
    public SolverGurobi(MPModel input) {
        this(input, null, true);
    }

    /**
     * Instantiates a new Gurobi solver.
     *
     * @param input        the input model
     * @param logToConsole true if Gurobi should print the output to the console
     *                     or false otherwise.
     */
    public SolverGurobi(MPModel input, boolean logToConsole) {
        this(input, null, logToConsole);
    }

    /**
     * Instantiates a new Gurobi solver.
     *
     * @param input        the input model
     * @param logFile      path of output file to save gurobi's log
     * @param logToConsole true if Gurobi should print the output to the console
     *                     or false otherwise.
     */
    public SolverGurobi(MPModel input, String logFile, boolean logToConsole) {
        super(input);

        initializeParams();
        extract(input);

        if (logFile != null) setParam(StringParam.LogFile, logFile);
        if (!logToConsole) {
            setParam(IntParam.LogToConsole, 0);
            try {
                gurobi.set(GRB.IntParam.OutputFlag, 0);
            }
            catch (GRBException e) {
                e.printStackTrace();
            }
        }
        input.setSolver(this);
    }

    /**
     * Gets the GRBModel object containing the model.
     *
     * @return the GRBModel object used by the wrapper
     */
    public GRBModel getGRBModel() {
        return gurobi;
    }


    @Override
    public void addSolution(double solution[]) {
        try {
            updateVars();
            gurobi.set(GRB.DoubleAttr.Start, vars, solution);
        }
        catch (GRBException e) {
            System.err.println("Error adding MIP start solution for model " + input.getName());
            e.printStackTrace();
        }
    }

    @Override
    public void addSolution(MPVar[] variables, double[] solution) {
        try {
            GRBVar grbVars[] = new GRBVar[variables.length];
            for (int i = 0; i < variables.length; i++)
                grbVars[i] = gurobi.getVar(variables[i].getIndex());

            gurobi.set(GRB.DoubleAttr.Start, grbVars, solution);
        }
        catch (GRBException e) {
            System.err.println("Error adding MIP start solution for model " + input.getName());
            e.printStackTrace();
        }
    }

    @Override
    public double getReducedCost(MPVar variable) {
        try {
            return reducedCosts[variable.getIndex()];
        }
        catch (Exception e) {
            System.err.println("Error obtaining reduced cost value for variable " + variable.getName());
            e.printStackTrace();
            throw new Error("Error obtaining reduced cost value for variable " + variable.getName());
        }
    }

    @Override
    public boolean solve(boolean linearRelaxation) {
        try {
            reducedCosts = null;
            if (linearRelaxation && !relaxed) relaxModel();
            else if (!linearRelaxation && relaxed) unrelaxModel();

            gurobi.getEnv();

            gurobi.optimize();
            boolean status = gurobi.get(GRB.IntAttr.SolCount) > 0;
            if (status) {
                objValues.clear();
                solutions.clear();

                buildSolution();
                for (int s = 1; s < Math.min(populateMin, gurobi.get(GRB.IntAttr.SolCount)); s++)
                    buildSolution(s);

                if (linearRelaxation || !input.hasIntVar()) buildDuals();
            }

            return status;
        }
        catch (GRBException e) {
            System.err.println("Error while running gurobi.");
            e.printStackTrace();
        }
        return false;
    }

    @Override
    public boolean populate() {
        try {
            if (relaxed) unrelaxModel();

            gurobi.optimize();
            boolean status = gurobi.get(GRB.IntAttr.Status) == GRB.Status.OPTIMAL;
            if (status) {
                objValues.clear();
                solutions.clear();

                buildSolution();
                for (int s = 1; s < gurobi.get(GRB.IntAttr.SolCount); s++)
                    buildSolution(s);

                if (!input.hasIntVar()) buildDuals();
            }
            return status;
        }
        catch (GRBException e) {
            System.err.println("Error while running (populating) gurobi.");
            e.printStackTrace();
        }
        return false;
    }

    @Override
    public void setPriorities(int priorities[]) {
        try {
            updateVars();
            gurobi.set(GRB.IntAttr.BranchPriority, vars, priorities);
        }
        catch (GRBException e) {
            System.err.println("Error adding MIP start solution for model " + input.getName());
            e.printStackTrace();
        }
    }

    @Override
    public void setPriorities(MPVar[] variables, int[] priorities) {
        try {
            GRBVar grbVars[] = new GRBVar[variables.length];
            for (int i = 0; i < variables.length; i++)
                grbVars[i] = gurobi.getVar(variables[i].getIndex());

            gurobi.set(GRB.IntAttr.BranchPriority, grbVars, priorities);
        }
        catch (GRBException e) {
            System.err.println("Error adding MIP start solution for model " + input.getName());
            e.printStackTrace();
        }
    }

    @Override
    public void updateModel() {
        try {
            // dealing with variables first
            for (MPAction action : input.actions()) {
                switch (action.type) {
                    case AddVar:
                        addVar(action.variable);
                        break;

                    case DelVar:
                        GRBVar varToDelete = gurobi.getVar(action.variable.getIndex());
                        gurobi.remove(varToDelete);
                        gurobi.update();
                        varUpdateRequired = true;
                        break;

                    case SetVarBounds:
                        GRBVar varToSetBounds = gurobi.getVar(action.variable.getIndex());
                        varToSetBounds.set(GRB.DoubleAttr.LB, action.variable.getLB());
                        varToSetBounds.set(GRB.DoubleAttr.UB, action.variable.getUB());
                        break;

                    case SetVarType:
                        GRBVar varToSetType = gurobi.getVar(action.variable.getIndex());
                        varToSetType.set(GRB.CharAttr.VType,
                          action.variable.getType() == MPVar.BINARY ? GRB.INTEGER :
                            action.variable.getType() == MPVar.BINARY ? GRB.BINARY : GRB.CONTINUOUS);
                        break;

                    default:
                        break;
                }
            }
            gurobi.update();

            // dealing with constraints
            for (MPAction action : input.actions()) {
                switch (action.type) {
                    case AddConstr:
                        addConstr(action.constr);
                        break;

                    case DelConstr:
                        GRBConstr constrToDelete = gurobi.getConstr(action.constr.getIndex());
                        gurobi.remove(constrToDelete);
                        gurobi.update();
                        break;

                    default:
                        break;
                }
            }
            gurobi.update();
        }
        catch (Exception e) {
            System.err.println("Error updating the model " + input.getName());
            e.printStackTrace();
        }
    }

    @Override
    public void updateObjective() {
        try {
            objective.clear();
            objective.addConstant(input.getObjective().getConstant());

            for (Map.Entry<MPVar, Double> entry : input.getObjective().coeffs())
                objective.addTerm(entry.getValue(), gurobi.getVar(entry.getKey().getIndex()));

            gurobi.setObjective(objective, input.getObjective().getDirection() == MPObjective.MINIMIZE ? GRB.MINIMIZE : GRB.MAXIMIZE);
            gurobi.update();
        }
        catch (GRBException e) {
            System.err.println("Error updating the objective function of model " + input.getName());
            e.printStackTrace();
        }
    }

    @Override
    public void writeModel(String filename) {
        try {
            if (relaxed) unrelaxModel();
            gurobi.write(filename);
        }
        catch (GRBException e) {
            System.err.println("Error while writing model to file " + filename);
            e.printStackTrace();
        }
    }


    /**
     * Extract.
     *
     * @param input the input
     */
    private void extract(MPModel input) {
        this.input = input;

        try {
            GRBEnv env = new GRBEnv();
            env.set(GRB.IntParam.Method, GRB.METHOD_DETERMINISTIC_CONCURRENT);

            gurobi = new GRBModel(env);
            gurobi.set(GRB.StringAttr.ModelName, input.getName());

            createVariables();
            createObjective();
            createConstraints();
        }
        catch (GRBException e) {
            System.err.println("Error while extracting model " + input.getName() + " to gurobi.");
            e.printStackTrace();
        }
    }


    private void buildDuals() throws GRBException {
        duals = new double[gurobi.get(GRB.IntAttr.NumConstrs)];
        for (int c = 0; c < duals.length; c++)
            duals[c] = gurobi.getConstr(c).get(GRB.DoubleAttr.Pi);
    }

    private void buildSolution() throws GRBException {
        buildSolution(-1);
    }

    private void buildSolution(int index) throws GRBException {
        if (index < 0) {
            updateVars();
            if (relaxed || (binVars.size() == 0 && intVars.size() == 0))
                reducedCosts = gurobi.get(GRB.DoubleAttr.RC, vars);
            objValue = gurobi.get(GRB.DoubleAttr.ObjVal);
            solution = new double[input.getNVars()];

            for (int v = 0; v < solution.length; v++)
                solution[v] = gurobi.getVar(v).get(GRB.DoubleAttr.X);

            objValues.add(objValue);
            solutions.add(solution);
        }
        else {
            double obj = objective.getConstant();
            gurobi.getEnv().set(GRB.IntParam.SolutionNumber, index);

            solutions.add(new double[input.getNVars()]);
            for (int v = 0; v < solution.length; v++) {
                solutions.get(solutions.size() - 1)[v] = gurobi.getVar(v).get(GRB.DoubleAttr.Xn);
                obj += gurobi.getVar(v).get(GRB.DoubleAttr.Xn) * gurobi.getVar(v).get(GRB.DoubleAttr.Obj);
            }
            objValues.add(obj);
        }
    }

    private void updateVars() {
        if (varUpdateRequired) {
            varUpdateRequired = false;
            vars = gurobi.getVars();
        }
    }

    /* Relaxing and (un)relaxing the formulation */

    private void relaxModel() throws GRBException {
        for (GRBVar binVar : binVars)
            binVar.set(GRB.CharAttr.VType, GRB.CONTINUOUS);

        for (GRBVar intVar : intVars)
            intVar.set(GRB.CharAttr.VType, GRB.CONTINUOUS);

        gurobi.update();
        relaxed = true;
    }

    private void unrelaxModel() throws GRBException {
        for (GRBVar binVar : binVars)
            binVar.set(GRB.CharAttr.VType, GRB.BINARY);

        for (GRBVar intVar : intVars)
            intVar.set(GRB.CharAttr.VType, GRB.INTEGER);

        gurobi.update();
        relaxed = false;
    }

    /* Creation of constraints, variables and objective function */

    private void addConstr(MPLinConstr constr) throws GRBException {
        updateVars();

        GRBLinExpr expr = new GRBLinExpr();
        expr.addConstant(constr.getConstant());

        for (Map.Entry<MPVar, Double> entry : constr.coeffs())
            expr.addTerm(entry.getValue(), vars[entry.getKey().getIndex()]);

        if (constr.getSense() == MPLinConstr.EQ)
            gurobi.addConstr(expr, GRB.EQUAL, 0.0, constr.getName());
        else if (constr.getSense() == MPLinConstr.LE)
            gurobi.addConstr(expr, GRB.LESS_EQUAL, 0.0, constr.getName());
        else if (constr.getSense() == MPLinConstr.GE)
            gurobi.addConstr(expr, GRB.GREATER_EQUAL, 0.0, constr.getName());
    }

    private void addVar(MPVar inputVariable) throws GRBException {
        varUpdateRequired = true;

        char type = (inputVariable.getType() == MPVar.BINARY) ? GRB.BINARY : (inputVariable.getType() == MPVar.INTEGER) ? GRB.INTEGER : GRB.CONTINUOUS;
        double obj = inputVariable.getObj();

        GRBColumn column = new GRBColumn();
        for (Map.Entry<MPLinConstr, Double> entry : inputVariable.coeffs())
            if (gurobi.getConstrs().length > entry.getKey().getIndex())
                column.addTerm(entry.getValue(), gurobi.getConstr(entry.getKey().getIndex()));

        GRBVar var;
        if (relaxed)
            var = gurobi.addVar(inputVariable.getLB(), inputVariable.getUB(), obj, GRB.CONTINUOUS, column, inputVariable.getName());
        else
            var = gurobi.addVar(inputVariable.getLB(), inputVariable.getUB(), obj, type, column, inputVariable.getName());

        if (type == GRB.BINARY) binVars.add(var);
        else if (type == GRB.INTEGER) intVars.add(var);
    }

    private void createConstraints() throws GRBException {
        for (MPLinConstr inputConstr : input.contrs())
            addConstr(inputConstr);

        gurobi.update();
    }

    private void createObjective() throws GRBException {
        updateVars();
        objective = new GRBLinExpr();
        objective.addConstant(input.getObjective().getConstant());

        for (Map.Entry<MPVar, Double> entry : input.getObjective().coeffs())
            objective.addTerm(entry.getValue(), gurobi.getVar(entry.getKey().getIndex()));

        gurobi.setObjective(objective, input.getObjective().getDirection() == MPObjective.MINIMIZE ? GRB.MINIMIZE : GRB.MAXIMIZE);
    }

    private void createVariables() throws GRBException {
        for (MPVar variable : input.vars())
            addVar(variable);

        gurobi.update();
        vars = gurobi.getVars();
    }

    // region Parameters initializer, getters and setters

    /**
     * Initialize params.
     */
    public void initializeParams() {
        booleanParams.put(BooleanParam.NumericalEmphasis, GRB.IntParam.NumericFocus);

        doubleParams.put(DoubleParam.CutLo, GRB.DoubleParam.Cutoff);
        doubleParams.put(DoubleParam.CutUp, GRB.DoubleParam.Cutoff);
        doubleParams.put(DoubleParam.Cutoff, GRB.DoubleParam.Cutoff);
        doubleParams.put(DoubleParam.MIPGap, GRB.DoubleParam.MIPGap);
        doubleParams.put(DoubleParam.MIPGapAbs, GRB.DoubleParam.MIPGapAbs);
        doubleParams.put(DoubleParam.TimeLimit, GRB.DoubleParam.TimeLimit);

        //intParams.put(IntParam.PopulateLim, GRB.IntParam...);
        //intParams.put(IntParam.RootAlg, GRB.IntParam.Roo);
        intParams.put(IntParam.LogToConsole, GRB.IntParam.LogToConsole);
        intParams.put(IntParam.Threads, GRB.IntParam.Threads);

        longParams.put(LongParam.IntSolLim, GRB.IntParam.SolutionLimit);

        stringParams.put(StringParam.LogFile, GRB.StringParam.LogFile);
    }

    @Override
    public boolean getParam(BooleanParam param) {
        try {
            return gurobi.getEnv().get(booleanParams.get(param)) == 1;
        }
        catch (GRBException e) {
            System.err.println("Error while getting parameter " + param);
            e.printStackTrace();
        }
        catch (NullPointerException e) {
            System.err.println("Warning: parameter " + param + " is not supported by this solver.");
        }
        return false;
    }

    @Override
    public double getParam(DoubleParam param) {
        try {
            return gurobi.getEnv().get(doubleParams.get(param));
        }
        catch (GRBException e) {
            System.err.println("Error while getting parameter " + param);
            e.printStackTrace();
        }
        catch (NullPointerException e) {
            System.err.println("Warning: parameter " + param + " is not supported by this solver.");
        }
        return -1;
    }

    @Override
    public int getParam(IntParam param) {
        try {
            return gurobi.getEnv().get(intParams.get(param));
        }
        catch (GRBException e) {
            System.err.println("Error while getting parameter " + param);
            e.printStackTrace();
        }
        catch (NullPointerException e) {
            System.err.println("Warning: parameter " + param + " is not supported by this solver.");
        }
        return -1;
    }

    @Override
    public long getParam(LongParam param) {
        try {
            return gurobi.getEnv().get(longParams.get(param));
        }
        catch (GRBException e) {
            System.err.println("Error while getting parameter " + param);
            e.printStackTrace();
        }
        catch (NullPointerException e) {
            System.err.println("Warning: parameter " + param + " is not supported by this solver.");
        }
        return -1;
    }

    @Override
    public String getParam(StringParam param) {
        try {
            return gurobi.getEnv().get(stringParams.get(param));
        }
        catch (GRBException e) {
            System.err.println("Error while getting parameter " + param);
            e.printStackTrace();
        }
        catch (NullPointerException e) {
            System.err.println("Warning: parameter " + param + " is not supported by this solver.");
        }
        return null;
    }

    @Override
    public void setParam(BooleanParam param, boolean value) {
        try {
            gurobi.getEnv().set(booleanParams.get(param), value ? 1 : 0);
        }
        catch (GRBException e) {
            System.err.println("Error while setting parameter " + param);
            e.printStackTrace();
        }
        catch (NullPointerException e) {
            System.err.println("Warning: parameter " + param + " is not supported by this solver.");
        }
    }

    @Override
    public void setParam(DoubleParam param, double value) {
        try {
            gurobi.getEnv().set(doubleParams.get(param), value);
        }
        catch (GRBException e) {
            System.err.println("Error while setting parameter " + param);
            e.printStackTrace();
        }
        catch (NullPointerException e) {
            System.err.println("Warning: parameter " + param + " is not supported by this solver.");
        }
    }

    @Override
    public void setParam(IntParam param, int value) {
        if (param == IntParam.PopulateLim)
            populateMin = value;
        else {
            try {
                gurobi.getEnv().set(intParams.get(param), value);
            }
            catch (GRBException e) {
                System.err.println("Error while setting parameter " + param);
                e.printStackTrace();
            }
            catch (NullPointerException e) {
                System.err.println("Warning: parameter " + param + " is not supported by this solver.");
            }
        }
    }

    @Override
    public void setParam(LongParam param, long value) {
        try {
            gurobi.getEnv().set(longParams.get(param), ( int ) value);
        }
        catch (GRBException e) {
            System.err.println("Error while setting parameter " + param);
            e.printStackTrace();
        }
        catch (NullPointerException e) {
            System.err.println("Warning: parameter " + param + " is not supported by this solver.");
        }
    }

    @Override
    public void setParam(StringParam param, String value) {
        try {
            gurobi.getEnv().set(stringParams.get(param), value);
        }
        catch (GRBException e) {
            System.err.println("Error while setting parameter " + param);
            e.printStackTrace();
        }
        catch (NullPointerException e) {
            System.err.println("Warning: parameter " + param + " is not supported by this solver.");
        }
    }

    // endregion
}

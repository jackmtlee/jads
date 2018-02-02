package jads.mp.solvers;

import ilog.concert.*;
import ilog.cplex.*;
import jads.mp.*;

import java.io.*;
import java.util.*;

/**
 * This class is a simple wrapper to use the solver CPLEX.
 *
 * @author Tulio Toffolo
 */
public class SolverCplex extends MPSolver {

    private IloCplex cplex;
    private ArrayList<IloRange> constrs = new ArrayList<IloRange>();
    private ArrayList<IloNumVar> vars = new ArrayList<IloNumVar>();
    private IloObjective objective;

    // region Parameters variables
    private HashMap<BooleanParam, IloCplex.BooleanParam> booleanParams = new HashMap<BooleanParam, IloCplex.BooleanParam>();
    private HashMap<DoubleParam, IloCplex.DoubleParam> doubleParams = new HashMap<DoubleParam, IloCplex.DoubleParam>();
    private HashMap<IntParam, IloCplex.IntParam> intParams = new HashMap<IntParam, IloCplex.IntParam>();
    private HashMap<LongParam, IloCplex.LongParam> longParams = new HashMap<LongParam, IloCplex.LongParam>();
    private HashMap<StringParam, IloCplex.StringParam> stringParams = new HashMap<StringParam, IloCplex.StringParam>();
    // endregion Parameters variables


    /**
     * Instantiates a new Solver cplex.
     *
     * @param input the input model
     */
    public SolverCplex(MPModel input) {
        super(input);
        initializeParams();

        extract(input);
        input.setSolver(this);
    }

    /**
     * Instantiates a new SolverCplex.
     *
     * @param input        the input model
     * @param logToConsole true if Cplex should print the output to System.out
     *                     or false otherwise.
     */
    public SolverCplex(MPModel input, boolean logToConsole) {
        super(input);
        initializeParams();

        extract(input);
        cplex.setOut(logToConsole ? System.out : null);
        cplex.setWarning(logToConsole ? System.err : null);
        input.setSolver(this);
    }

    /**
     * Instantiates a new SolverCplex.
     *
     * @param input         the input model
     * @param outputStream  the output stream for logging purposes
     * @param warningStream the warning stream for logging purposes
     */
    public SolverCplex(MPModel input, OutputStream outputStream, OutputStream warningStream) {
        super(input);
        initializeParams();

        extract(input);
        cplex.setOut(outputStream);
        cplex.setWarning(warningStream);
        input.setSolver(this);
    }


    /**
     * Gets the IloCplex object containing the Cplex solver.
     *
     * @return the IloCplex object used by the wrapper
     */
    public IloCplex getIloCplex() {
        return cplex;
    }

    @Override
    public void addSolution(double solution[]) {
        try {
            cplex.addMIPStart(vars.toArray(new IloNumVar[vars.size()]), solution);
        }
        catch (IloException e) {
            System.err.println("Error adding MIP start solution for model " + input.getName());
            e.printStackTrace();
        }
    }

    @Override
    public void addSolution(MPVar variables[], double solution[]) {
        try {
            IloNumVar iloVars[] = new IloNumVar[variables.length];
            for (int i = 0; i < variables.length; i++)
                iloVars[i] = this.vars.get(variables[i].getIndex());

            cplex.addMIPStart(iloVars, solution);
        }
        catch (IloException e) {
            System.err.println("Error adding MIP start solution for model " + input.getName());
            e.printStackTrace();
        }
    }

    @Override
    public double getReducedCost(MPVar variable) {
        try {
            return cplex.getReducedCost(vars.get(variable.getIndex()));
        }
        catch (IloException e) {
            System.err.println("Error obtaining reduced cost value for variable " + variable.getName());
            e.printStackTrace();
            throw new Error("Error obtaining reduced cost value for variable " + variable.getName());
        }
    }

    @Override
    public void setPriorities(int priorities[]) {
        try {
            cplex.setPriorities(vars.toArray(new IloNumVar[vars.size()]), priorities);
        }
        catch (IloException e) {
            System.err.println("Error setting branching priorities for model " + input.getName());
            e.printStackTrace();
        }
    }

    @Override
    public void setPriorities(MPVar variables[], int priorities[]) {
        try {
            IloNumVar iloVars[] = new IloNumVar[variables.length];
            for (int i = 0; i < variables.length; i++)
                iloVars[i] = this.vars.get(variables[i].getIndex());

            cplex.setPriorities(iloVars, priorities);
        }
        catch (IloException e) {
            System.err.println("Error setting branching priorities for model " + input.getName());
            e.printStackTrace();
        }
    }

    @Override
    public boolean solve(boolean linearRelaxation) {
        objValues.clear();
        solutions.clear();

        try {
            ArrayList<IloConversion> conversions = null;
            if (linearRelaxation) {
                conversions = new ArrayList<IloConversion>();
                for (MPVar inputVariable : input.vars()) {
                    if (inputVariable.isInteger()) {
                        IloConversion conversion = cplex.conversion(vars.get(inputVariable.getIndex()), IloNumVarType.Float);
                        conversions.add(conversion);
                    }
                }
                cplex.add(conversions.toArray(new IloConversion[conversions.size()]));
            }

            boolean status = cplex.solve();

            if (status) {
                buildSolution();
                for (int s = 1; s < cplex.getSolnPoolNsolns(); s++)
                    buildSolution(s);

                if (linearRelaxation || !input.hasIntVar()) buildDuals();
            }

            if (linearRelaxation)
                for (IloConversion conversion : conversions)
                    cplex.remove(conversion);

            return status;
        }
        catch (IloException e) {
            System.err.println("Error while running cplex.");
            e.printStackTrace();
        }
        return false;
    }

    @Override
    public boolean populate() {
        objValues.clear();
        solutions.clear();
        try {
            boolean status = cplex.populate();
            if (status) {
                buildSolution();
                int nSolutions = Math.min(getParam(IntParam.PopulateLim), cplex.getSolnPoolNsolns());
                for (int s = 1; s < nSolutions; s++)
                    buildSolution(s);

                if (!input.hasIntVar()) buildDuals();
                return true;
            }
            return false;
        }
        catch (IloException e) {
            System.err.println("Error while running (populating) cplex.");
            e.printStackTrace();
        }
        return false;
    }

    @Override
    public void writeModel(String filename) {
        try {
            cplex.exportModel(filename);
        }
        catch (IloException e) {
            System.err.println("Error while writing model to file " + filename);
            e.printStackTrace();
        }
    }


    @Override
    protected void updateModel() {
        try {
            ArrayList<IloConversion> conversions = new ArrayList<IloConversion>();
            for (MPAction action : input.actions()) {
                switch (action.type) {
                    case AddConstr:
                        addConstraint(action.constr);
                        break;

                    case AddVar:
                        addVar(action.variable);
                        break;

                    case DelConstr:
                        IloRange constrToDelete = constrs.remove(action.constr.getIndex());
                        cplex.delete(constrToDelete);
                        break;

                    case DelVar:
                        IloNumVar varToDelete = vars.remove(action.variable.getIndex());
                        cplex.delete(varToDelete);
                        break;

                    case SetVarBounds:
                        IloNumVar varToSetBounds = vars.get(action.variable.getIndex());
                        varToSetBounds.setLB(action.variable.getLB());
                        varToSetBounds.setUB(action.variable.getUB());
                        break;

                    case SetVarType:
                        IloNumVar varToSetType = vars.get(action.variable.getIndex());
                        IloConversion conversion = cplex.conversion(varToSetType,
                          action.variable.getType() == MPVar.BINARY ? IloNumVarType.Int :
                            action.variable.getType() == MPVar.BINARY ? IloNumVarType.Bool : IloNumVarType.Float);
                        conversions.add(conversion);
                        break;
                }
            }
            cplex.add(conversions.toArray(new IloConversion[conversions.size()]));
        }
        catch (IloException e) {
            System.err.println("Error updating the objective function of model " + input.getName());
            e.printStackTrace();
        }
    }

    @Override
    protected void updateObjective() {
        try {
            IloLinearNumExpr expr = cplex.linearNumExpr(input.getObjective().getConstant());

            for (Map.Entry<MPVar, Double> entry : input.getObjective().coeffs())
                expr.addTerm(entry.getValue(), vars.get(entry.getKey().getIndex()));

            objective.setExpr(expr);
        }
        catch (IloException e) {
            System.err.println("Error updating the objective function of model " + input.getName());
            e.printStackTrace();
        }
    }


    private void buildDuals() throws IloException {
        duals = cplex.getDuals(constrs.toArray(new IloRange[constrs.size()]));
    }

    private void extract(MPModel input) {
        this.input = input;

        try {
            cplex = new IloCplex();
            cplex.setName(input.getName());

            createVariables();
            createConstraints();
            createObjective();
        }
        catch (IloException e) {
            System.err.println("Error while extracting model " + input.getName() + " to cplex.");
            e.printStackTrace();
        }
    }

    /* Build duals and solution(s) */

    private void buildSolution() throws IloException {
        buildSolution(-1);
    }

    private void buildSolution(int index) throws IloException {
        if (index < 0) {
            objValue = cplex.getObjValue();
            solution = new double[vars.size()];
            for (int i = 0; i < vars.size(); i++) {
                try {
                    solution[i] = cplex.getValue(vars.get(i));
                }
                catch (IloCplex.UnknownObjectException e) {
                    solution[i] = 0;
                }
            }
            //IloNumVar varArray[] = vars.toArray(new IloNumVar[vars.size()]);
            //solution = cplex.getValues(varArray);

            objValues.add(objValue);
            solutions.add(solution);
        }
        else {
            objValues.add(cplex.getObjValue(index));
            double solutionIndex[] = new double[vars.size()];
            for (int i = 0; i < vars.size(); i++) {
                try {
                    solutionIndex[i] = cplex.getValue(vars.get(i), index);
                }
                catch (IloCplex.UnknownObjectException e) {
                    solutionIndex[i] = 0;
                }
            }
            solutions.add(solutionIndex);
        }
    }

    /* Creation of constraints, variables and objective function */

    private void addConstraint(MPLinConstr inputConstr) throws IloException {
        IloLinearNumExpr expr = cplex.linearNumExpr(inputConstr.getConstant());

        for (Map.Entry<MPVar, Double> entry : inputConstr.coeffs())
            expr.addTerm(entry.getValue(), vars.get(entry.getKey().getIndex()));

        if (inputConstr.getSense() == MPLinConstr.EQ)
            constrs.add(cplex.addEq(expr, 0., inputConstr.getName()));
        else if (inputConstr.getSense() == MPLinConstr.LE)
            constrs.add(cplex.addLe(expr, 0., inputConstr.getName()));
        else if (inputConstr.getSense() == MPLinConstr.GE)
            constrs.add(cplex.addGe(expr, 0., inputConstr.getName()));
    }

    private void addVar(MPVar inputVariable) throws IloException {
        IloColumn column = cplex.column(objective, inputVariable.getObj());
        for (Map.Entry<MPLinConstr, Double> entry : inputVariable.coeffs())
            if (constrs.size() > entry.getKey().getIndex())
                column = column.and(cplex.column(constrs.get(entry.getKey().getIndex()), entry.getValue()));

        if (inputVariable.getType() == MPVar.BINARY && inputVariable.getLB() == 0. && inputVariable.getUB() == 1.)
            vars.add(cplex.boolVar(column, inputVariable.getName()));
        else if (inputVariable.getType() == MPVar.INTEGER)
            vars.add(cplex.intVar(column, ( int ) Math.round(inputVariable.getLB()), ( int ) Math.round(inputVariable.getUB()), inputVariable.getName()));
        else
            vars.add(cplex.numVar(column, inputVariable.getLB(), inputVariable.getUB(), inputVariable.getName()));
    }

    private void createConstraints() throws IloException {
        for (MPLinConstr inputConstr : input.contrs())
            addConstraint(inputConstr);
    }

    private void createObjective() throws IloException {
        MPObjective inputObjective = input.getObjective();
        IloLinearNumExpr expr = cplex.linearNumExpr(inputObjective.getConstant());

        for (Map.Entry<MPVar, Double> entry : inputObjective.coeffs())
            expr.addTerm(entry.getValue(), vars.get(entry.getKey().getIndex()));

        if (inputObjective.getDirection() == MPObjective.MINIMIZE)
            objective = cplex.addMinimize(expr, inputObjective.getName());
        else
            objective = cplex.addMaximize(expr, inputObjective.getName());
    }

    private void createVariables() throws IloException {
        for (MPVar inputVariable : input.vars()) {
            if (inputVariable.getType() == MPVar.BINARY && inputVariable.getLB() == 0. && inputVariable.getUB() == 1.)
                vars.add(cplex.boolVar(inputVariable.getName()));
            else if (inputVariable.getType() == MPVar.INTEGER)
                vars.add(cplex.intVar(( int ) Math.round(inputVariable.getLB()), ( int ) Math.round(inputVariable.getUB()), inputVariable.getName()));
            else
                vars.add(cplex.numVar(inputVariable.getLB(), inputVariable.getUB(), inputVariable.getName()));
        }
    }

    // region Parameters getters and setters

    private void initializeParams() {
        booleanParams.put(BooleanParam.NumericalEmphasis, IloCplex.BooleanParam.NumericalEmphasis);

        doubleParams.put(DoubleParam.CutLo, IloCplex.DoubleParam.CutLo);
        doubleParams.put(DoubleParam.CutUp, IloCplex.DoubleParam.CutUp);
        doubleParams.put(DoubleParam.MIPGap, IloCplex.DoubleParam.EpGap);
        doubleParams.put(DoubleParam.MIPGapAbs, IloCplex.DoubleParam.EpAGap);
        doubleParams.put(DoubleParam.ObjDif, IloCplex.DoubleParam.ObjDif);
        doubleParams.put(DoubleParam.RelObjDif, IloCplex.DoubleParam.RelObjDif);
        doubleParams.put(DoubleParam.TimeLimit, IloCplex.DoubleParam.TimeLimit);

        intParams.put(IntParam.PopulateLim, IloCplex.IntParam.PopulateLim);
        intParams.put(IntParam.RootAlg, IloCplex.IntParam.RootAlg);
        intParams.put(IntParam.Threads, IloCplex.IntParam.Threads);

        longParams.put(LongParam.IntSolLim, IloCplex.LongParam.IntSolLim);
        longParams.put(LongParam.IterLimit, IloCplex.LongParam.ItLim);
    }

    @Override
    public boolean getParam(BooleanParam param) {
        try {
            return cplex.getParam(booleanParams.get(param));
        }
        catch (IloException e) {
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
            return cplex.getParam(doubleParams.get(param));
        }
        catch (IloException e) {
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
            return cplex.getParam(intParams.get(param));
        }
        catch (IloException e) {
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
            return cplex.getParam(longParams.get(param));
        }
        catch (IloException e) {
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
            return cplex.getParam(stringParams.get(param));
        }
        catch (IloException e) {
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
            cplex.setParam(booleanParams.get(param), value);
        }
        catch (IloException e) {
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
            cplex.setParam(doubleParams.get(param), value);
        }
        catch (IloException e) {
            System.err.println("Error while setting parameter " + param);
            e.printStackTrace();
        }
        catch (NullPointerException e) {
            System.err.println("Warning: parameter " + param + " is not supported by this solver.");
        }
    }

    @Override
    public void setParam(IntParam param, int value) {
        try {
            cplex.setParam(intParams.get(param), value);
        }
        catch (IloException e) {
            System.err.println("Error while setting parameter " + param);
            e.printStackTrace();
        }
        catch (NullPointerException e) {
            System.err.println("Warning: parameter " + param + " is not supported by this solver.");
        }
    }

    @Override
    public void setParam(LongParam param, long value) {
        try {
            cplex.setParam(longParams.get(param), value);
        }
        catch (IloException e) {
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
            cplex.setParam(stringParams.get(param), value);
        }
        catch (IloException e) {
            System.err.println("Error while setting parameter " + param);
            e.printStackTrace();
        }
        catch (NullPointerException e) {
            System.err.println("Warning: parameter " + param + " is not supported by this solver.");
        }
    }

    // endregion
}

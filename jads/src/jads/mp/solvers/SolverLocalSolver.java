//package jads.mp.solvers;
//
//import jads.mp.*;
//import localsolver.*;
//
//import java.util.*;
//
///**
// * This class is a simple wrapper to use LocalSolver to solve the formulation.
// *
// * @author Tulio Toffolo
// */
//public class SolverLocalSolver extends MPSolver {
//
//    private LocalSolver localsolver;
//    private LSModel model;
//    private ArrayList<LSExpression> constrs = new ArrayList<LSExpression>();
//    private ArrayList<LSExpression> vars = new ArrayList<LSExpression>();
//    private LSExpression objective;
//
//    // region Parameters variables
//    private int annealingLevel = -1;
//    private long iterLimit = -1;
//    private String logFile = null;
//    private double objBound = -1;
//    private int threads = -1;
//    private int timeLimit = -1;
//    private int verbosity = -1;
//    // endregion Parameters variables
//
//
//    /**
//     * Instantiates a new LocalSolver solver.
//     *
//     * @param input the input model
//     */
//    public SolverLocalSolver(MPModel input) {
//        super(input);
//        initializeParams();
//
//        extract(input);
//    }
//
//    /**
//     * Gets the {@link LocalSolver} object.
//     *
//     * @return the actual {@link LocalSolver} object
//     */
//    public LocalSolver getLocalSolver() {
//        return localsolver;
//    }
//
//
//    @Override
//    public void addSolution(double solution[]) {
//        for (int i = 0; i < vars.size(); i++)
//            vars.get(i).setValue(solution[i]);
//
//        //try {
//        //    model.addMIPStart(vars.toArray(new IloNumVar[vars.size()]), solution);
//        //}
//        //catch (IloException e) {
//        //    System.err.println("Error adding MIP start solution for model " + input.getName());
//        //    e.printStackTrace();
//        //}
//    }
//
//    @Override
//    public void addSolution(MPVar[] variables, double[] solution) {
//        for (int i = 0; i < variables.length; i++)
//            vars.get(variables[i].getIndex()).setValue(solution[i]);
//    }
//
//    /**
//     * Extract.
//     *
//     * @param input the input
//     */
//    public void extract(MPModel input) {
//        this.input = input;
//
//        localsolver = new LocalSolver();
//        model = localsolver.getModel();
//
//        createVariables();
//        createConstraints();
//        createObjective();
//    }
//
//    /**
//     * Sets annealing level.
//     *
//     * @param level the level
//     */
//    public void setAnnealingLevel(int level) {
//        this.annealingLevel = level;
//    }
//
//    @Override
//    public void setPriorities(int[] priorities) {
//        throw new Error("Method not implemented");
//
//        //try {
//        //    model.setPriorities(vars.toArray(new IloNumVar[vars.size()]), priorities);
//        //}
//        //catch (IloException e) {
//        //    System.err.println("Error setting branching priorities for model " + input.getName());
//        //    e.printStackTrace();
//        //}
//    }
//
//    @Override
//    public void setPriorities(MPVar[] variables, int[] priorities) {
//        throw new Error("Method not implemented");
//
//        //try {
//        //    IloNumVar iloVars[] = new IloNumVar[variables.length];
//        //    for (int i = 0; i < variables.length; i++)
//        //        iloVars[i] = this.vars.get(variables[i].getIndex());
//        //
//        //    model.setPriorities(iloVars, priorities);
//        //}
//        //catch (IloException e) {
//        //    System.err.println("Error setting branching priorities for model " + input.getName());
//        //    e.printStackTrace();
//        //}
//    }
//
//    @Override
//    public boolean solve(boolean linearRelaxation) {
//        if (linearRelaxation)
//            throw new Error("Solving the linear relaxation is currently not supported in LocalSolver");
//
//        // closing the model as required by LocalSolver
//        model.close();
//
//        // setting parameters of LocalSolver
//        if (annealingLevel >= 0)
//            localsolver.getParam().setAnnealingLevel(annealingLevel);
//        if (logFile != null)
//            localsolver.getParam().setLogFile(logFile);
//        if (objBound >= 0)
//            localsolver.getParam().setObjectiveBound(0, objBound);
//        if (threads >= 0)
//            localsolver.getParam().setNbThreads(threads);
//        if (verbosity >= 0)
//            localsolver.getParam().setVerbosity(verbosity);
//
//        // running LocalSolver
//        LSPhase phase = localsolver.createPhase();
//        if (iterLimit >= 0) phase.setIterationLimit(iterLimit);
//        if (timeLimit >= 0) phase.setTimeLimit(timeLimit);
//        localsolver.solve();
//
//        boolean status = localsolver.getSolution().getStatus().equals(LSSolutionStatus.Feasible) || localsolver.getSolution().getStatus().equals(LSSolutionStatus.Optimal);
//        if (status) {
//            buildSolution();
//        }
//
//        return status;
//    }
//
//    @Override
//    public boolean populate() {
//        throw new Error("LocalSolver does not implement a method like populate()");
//    }
//
//    @Override
//    public void updateModel() {
//        throw new Error("Method not implemented");
//
//        //try {
//        //    for (Action action : input.actions()) {
//        //        switch (action.type) {
//        //            case AddConstr:
//        //                addConstraint(action.constr);
//        //                break;
//        //
//        //            case AddVar:
//        //                addVar(action.variable);
//        //                break;
//        //
//        //            case DelConstr:
//        //                IloRange constr = constrs.remove(action.constr.getIndex());
//        //                model.delete(constr);
//        //                break;
//        //
//        //            case DelVar:
//        //                IloNumVar var = vars.remove(action.variable.getIndex());
//        //                model.delete(var);
//        //                break;
//        //        }
//        //    }
//        //    input.setUpdated(true);
//        //}
//        //catch (IloException e) {
//        //    System.err.println("Error updating the objective function of model " + input.getName());
//        //    e.printStackTrace();
//        //}
//    }
//
//    @Override
//    public void updateObjective() {
//        objective = model.sum(input.getObjective().getConstant());
//
//        for (Map.Entry<MPVar, Double> entry : input.getObjective().coeffs())
//            objective.addOperand(model.prod(entry.getValue(), vars.get(entry.getKey().getIndex())));
//
//        if (input.getObjective().getDirection() == MPObjective.MINIMIZE)
//            model.minimize(objective);
//        else
//            model.maximize(objective);
//    }
//
//    @Override
//    public void writeModel(String filename) {
//        localsolver.saveEnvironment(filename);
//    }
//
//    /* Build duals and solution(s) */
//
//    private void buildDuals() {
//        throw new Error("LocalSolver cannot obtain dual values of constraints.");
//
//        //duals = model.getDuals(constrs.toArray(new IloRange[constrs.size()]));
//    }
//
//    private void buildSolution() {
//        objValue = localsolver.getSolution().getDoubleValue(objective);
//        solution = new double[vars.size()];
//        for (int i = 0; i < vars.size(); i++)
//            solution[i] = localsolver.getSolution().getDoubleValue(vars.get(i));
//    }
//
//    /* Creation of constraints, variables and objective function */
//
//    private void addConstraint(MPLinConstr inputConstr) {
//        LSExpression expr = model.sum(inputConstr.getConstant());
//
//        for (Map.Entry<MPVar, Double> entry : inputConstr.coeffs())
//            expr.addOperand(model.prod(entry.getValue(), vars.get(entry.getKey().getIndex())));
//
//        LSExpression constr = null;
//        if (inputConstr.getSense() == MPLinConstr.EQ)
//            constr = model.eq(expr, 0);
//        else if (inputConstr.getSense() == MPLinConstr.LE)
//            constr = model.leq(expr, 0);
//        else if (inputConstr.getSense() == MPLinConstr.GE)
//            constr = model.geq(expr, 0);
//
//        model.addConstraint(constr);
//        constrs.add(constr);
//    }
//
//    private void addVar(MPVar inputVariable) {
//        throw new Error("Method not implemented");
//
//        //IloColumn column = model.column(objective, inputVariable.getObj());
//        //for (Map.Entry<LinearConstr, Double> entry : inputVariable.coeffs())
//        //    if (constrs.size() > entry.getKey().getIndex())
//        //        column = column.and(model.column(constrs.get(entry.getKey().getIndex()), entry.getValue()));
//        //
//        //if (inputVariable.getType() == Variable.BINARY && inputVariable.getLB() == 0. && inputVariable.getUB() == 1.)
//        //    vars.add(model.boolVar(column, inputVariable.getName()));
//        //else if (inputVariable.getType() == Variable.INTEGER)
//        //    vars.add(model.intVar(column, ( int ) Math.round(inputVariable.getLB()), ( int ) Math.round(inputVariable.getUB()), inputVariable.getName()));
//        //else
//        //    vars.add(model.numVar(column, inputVariable.getLB(), inputVariable.getUB(), inputVariable.getName()));
//    }
//
//    private void createConstraints() {
//        for (MPLinConstr inputConstr : input.contrs())
//            addConstraint(inputConstr);
//    }
//
//    private void createObjective() {
//        MPObjective inputObjective = input.getObjective();
//        objective = model.sum(inputObjective.getConstant());
//
//        for (Map.Entry<MPVar, Double> entry : inputObjective.coeffs())
//            objective.addOperand(model.prod(entry.getValue(), vars.get(entry.getKey().getIndex())));
//
//        if (inputObjective.getDirection() == MPObjective.MINIMIZE)
//            model.minimize(objective);
//        else
//            model.maximize(objective);
//    }
//
//    private void createVariables() {
//        for (MPVar inputVariable : input.vars()) {
//            if (inputVariable.getType() == MPVar.BINARY && inputVariable.getLB() == 0. && inputVariable.getUB() == 1.)
//                vars.add(model.boolVar());
//            else if (inputVariable.getType() == MPVar.INTEGER)
//                vars.add(model.intVar(( int ) Math.round(inputVariable.getLB()), ( int ) Math.round(inputVariable.getUB())));
//            else
//                vars.add(model.floatVar(inputVariable.getLB(), inputVariable.getUB()));
//        }
//    }
//
//    // region Parameters getters and setters
//
//    private void initializeParams() { }
//
//    @Override
//    public boolean getParam(BooleanParam param) {
//        throw new Error("Parameter " + param + " is not supported by Local Solver.");
//    }
//
//    @Override
//    public double getParam(DoubleParam param) {
//        throw new Error("Parameter " + param + " is not supported by Local Solver.");
//    }
//
//    @Override
//    public int getParam(IntParam param) {
//        throw new Error("Parameter " + param + " is not supported by Local Solver.");
//    }
//
//    @Override
//    public long getParam(LongParam param) {
//        throw new Error("Parameter " + param + " is not supported by Local Solver.");
//    }
//
//    @Override
//    public String getParam(StringParam param) {
//        throw new Error("Parameter " + param + " is not supported by Local Solver.");
//    }
//
//    @Override
//    public void setParam(BooleanParam param, boolean value) {
//        throw new Error("Parameter " + param + " is not supported by Local Solver.");
//    }
//
//    @Override
//    public void setParam(DoubleParam param, double value) {
//        if (param == DoubleParam.TimeLimit)
//            timeLimit = ( int ) value;
//        else if (param == DoubleParam.Cutoff)
//            objBound = value;
//        else
//            throw new Error("Parameter " + param + " is not supported by Local Solver.");
//    }
//
//    @Override
//    public void setParam(IntParam param, int value) {
//        if (param == IntParam.LogToConsole)
//            verbosity = value;
//        else if (param == IntParam.Threads)
//            threads = value;
//        else
//            throw new Error("Parameter " + param + " is not supported by Local Solver.");
//    }
//
//    @Override
//    public void setParam(LongParam param, long value) {
//        if (param == LongParam.IterLimit)
//            iterLimit = value;
//        else
//            throw new Error("Parameter " + param + " is not supported by Local Solver.");
//    }
//
//    @Override
//    public void setParam(StringParam param, String value) {
//        if (param == StringParam.LogFile)
//            logFile = value;
//        else
//            throw new Error("Parameter " + param + " is not supported by Local Solver.");
//    }
//
//    // endregion
//}

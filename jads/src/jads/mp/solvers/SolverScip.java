//package jads.mp.solvers;
//
//import jads.mp.*;
//import de.zib.jscip.nativ.*;
//import de.zib.jscip.nativ.jni.*;
//
//import java.io.*;
//import java.util.*;
//
///**
// * This class is a simple wrapper to use the solver SCIP.
// *
// * @author Tulio Toffolo
// */
//public class SolverScip extends MPSolver {
//
//    /* create the SCIP environment */
//    private JniScip env = new JniScip();
//    /* create the SCIP variable environment */
//    private JniScipVar envVar = new JniScipVar();
//    /* create the SCIP  constraint environment */
//    private JniScipCons envCons = new JniScipCons();
//    /* create the SCIP knapsack constraint environment */
//    private JniScipConsKnapsack envConsKnapsack = new JniScipConsKnapsack();
//    /* create the SCIP linear constraint environment */
//    private JniScipConsLinear envConsLinear = new JniScipConsLinear();
//
//    private long scip;
//
//    private ArrayList<Long> constrs = new ArrayList<Long>();
//    private ArrayList<Long> vars = new ArrayList<Long>();
//
//    // region Parameters variables
//    private HashMap<BooleanParam, Long> booleanParams = new HashMap<BooleanParam, Long>();
//    private HashMap<DoubleParam, Long> doubleParams = new HashMap<DoubleParam, Long>();
//    private HashMap<IntParam, Long> intParams = new HashMap<IntParam, Long>();
//    private HashMap<LongParam, Long> longParams = new HashMap<LongParam, Long>();
//    private HashMap<StringParam, Long> stringParams = new HashMap<StringParam, Long>();
//    // endregion Parameters variables
//
//
//    /**
//     * Instantiates a new Solver cplex.
//     *
//     * @param input the input model
//     */
//    public SolverScip(MPModel input) {
//        super(input);
//        JniScipLibraryLoader.loadLibrary();
//
//        initializeParams();
//
//        extract(input);
//        input.setSolver(this);
//    }
//
//    /**
//     * Instantiates a new SolverCplex.
//     *
//     * @param input        the input model
//     * @param logToConsole true if Cplex should print the output to System.out
//     *                     or false otherwise.
//     */
//    public SolverScip(MPModel input, boolean logToConsole) {
//        super(input);
//        JniScipLibraryLoader.loadLibrary();
//
//        initializeParams();
//
//        extract(input);
//        try {
//            env.setMessagehdlrQuiet(scip, logToConsole);
//            //env.setOut(logToConsole ? System.out : null);
//            //env.setWarning(logToConsole ? System.err : null);
//        }
//        catch (NativeScipException e) {
//            e.printStackTrace();
//        }
//
//        input.setSolver(this);
//    }
//
//    /**
//     * Instantiates a new SolverCplex.
//     *
//     * @param input         the input model
//     * @param outputStream  the output stream for logging purposes
//     * @param warningStream the warning stream for logging purposes
//     */
//    public SolverScip(MPModel input, OutputStream outputStream, OutputStream warningStream) {
//        super(input);
//        JniScipLibraryLoader.loadLibrary();
//
//        initializeParams();
//
//        extract(input);
//        //env.setOut(outputStream);
//        //env.setWarning(warningStream);
//        input.setSolver(this);
//    }
//
//
//    @Override
//    public void addSolution(double solution[]) {
//        System.err.println("Error adding MIP start solution for model " + input.getName());
//    }
//
//    @Override
//    public void addSolution(MPVar variables[], double solution[]) {
//        System.err.println("Error adding MIP start solution for model " + input.getName());
//    }
//
//    @Override
//    public double getReducedCost(MPVar variable) {
//        System.err.println("Error obtaining reduced cost of variable " + variable.getName());
//
//        //try {
//        //    return env.getReducedCost(vars.get(variable.getIndex()));
//        //}
//        //catch (NativeScipException e) {
//        //    System.err.println("Error obtaining reduced cost value for variable " + variable.getName());
//        //    e.printStackTrace();
//        //    throw new Error("Error obtaining reduced cost value for variable " + variable.getName());
//        //}
//    }
//
//    @Override
//    public void setPriorities(int priorities[]) {
//        System.err.println("Error setting variable priorities");
//
//        //try {
//        //    env.setPriorities(vars.toArray(new JniScipVar[vars.size()]), priorities);
//        //}
//        //catch (NativeScipException e) {
//        //    System.err.println("Error setting branching priorities for model " + input.getName());
//        //    e.printStackTrace();
//        //}
//    }
//
//    @Override
//    public void setPriorities(MPVar variables[], int priorities[]) {
//        System.err.println("Error setting variable priorities");
//
//        //try {
//        //    JniScipVar iloVars[] = new JniScipVar[variables.length];
//        //    for (int i = 0; i < variables.length; i++)
//        //        iloVars[i] = this.vars.get(variables[i].getIndex());
//        //
//        //    env.setPriorities(iloVars, priorities);
//        //}
//        //catch (NativeScipException e) {
//        //    System.err.println("Error setting branching priorities for model " + input.getName());
//        //    e.printStackTrace();
//        //}
//    }
//
//    @Override
//    public boolean solve(boolean linearRelaxation) {
//        objValues.clear();
//        solutions.clear();
//
//        try {
//            ArrayList<IloConversion> conversions = null;
//            if (linearRelaxation) {
//                conversions = new ArrayList<IloConversion>();
//                for (MPVar inputVariable : input.vars()) {
//                    if (inputVariable.isInteger()) {
//                        IloConversion conversion = env.conversion(vars.get(inputVariable.getIndex()), JniScipVarType.Float);
//                        conversions.add(conversion);
//                    }
//                }
//                env.add(conversions.toArray(new IloConversion[conversions.size()]));
//            }
//
//            boolean status = env.solve();
//
//            if (status) {
//                buildSolution();
//                for (int s = 1; s < env.getSolnPoolNsolns(); s++)
//                    buildSolution(s);
//
//                if (linearRelaxation || !input.hasIntVar()) buildDuals();
//            }
//
//            if (linearRelaxation)
//                for (IloConversion conversion : conversions)
//                    env.remove(conversion);
//
//            return status;
//        }
//        catch (NativeScipException e) {
//            System.err.println("Error while running cplex.");
//            e.printStackTrace();
//        }
//        return false;
//    }
//
//    @Override
//    public boolean populate() {
//        System.err.println("Error: populate() is not available in SCIP.");
//    }
//
//    @Override
//    public void writeModel(String filename) {
//        try {
//            env.writeLP(scip, filename);
//        }
//        catch (NativeScipException e) {
//            System.err.println("Error while writing model to file " + filename);
//            e.printStackTrace();
//        }
//    }
//
//
//    @Override
//    protected void updateModel() {
//        try {
//            ArrayList<IloConversion> conversions = new ArrayList<IloConversion>();
//            for (MPAction action : input.actions()) {
//                switch (action.type) {
//                    case AddConstr:
//                        addConstraint(action.constr);
//                        break;
//
//                    case AddVar:
//                        addVar(action.variable);
//                        break;
//
//                    case DelConstr:
//                        JniScipCons constrToDelete = constrs.remove(action.constr.getIndex());
//                        env.delete(constrToDelete);
//                        break;
//
//                    case DelVar:
//                        JniScipVar varToDelete = vars.remove(action.variable.getIndex());
//                        env.delete(varToDelete);
//                        break;
//
//                    case SetVarBounds:
//                        JniScipVar varToSetBounds = vars.get(action.variable.getIndex());
//                        varToSetBounds.setLB(action.variable.getLB());
//                        varToSetBounds.setUB(action.variable.getUB());
//                        break;
//
//                    case SetVarType:
//                        JniScipVar varToSetType = vars.get(action.variable.getIndex());
//                        IloConversion conversion = env.conversion(varToSetType,
//                          action.variable.getType() == MPVar.BINARY ? JniScipVarType.Int :
//                            action.variable.getType() == MPVar.BINARY ? JniScipVarType.Bool : JniScipVarType.Float);
//                        conversions.add(conversion);
//                        break;
//                }
//            }
//            env.add(conversions.toArray(new IloConversion[conversions.size()]));
//        }
//        catch (NativeScipException e) {
//            System.err.println("Error updating the objective function of model " + input.getName());
//            e.printStackTrace();
//        }
//    }
//
//    @Override
//    protected void updateObjective() {
//        try {
//            IloLinearNumExpr expr = env.linearNumExpr(input.getObjective().getConstant());
//
//            for (Map.Entry<MPVar, Double> entry : input.getObjective().coeffs())
//                expr.addTerm(entry.getValue(), vars.get(entry.getKey().getIndex()));
//
//            objective.setExpr(expr);
//        }
//        catch (NativeScipException e) {
//            System.err.println("Error updating the objective function of model " + input.getName());
//            e.printStackTrace();
//        }
//    }
//
//
//    private void buildDuals() throws NativeScipException {
//        duals = env.getDuals(constrs.toArray(new JniScipCons[constrs.size()]));
//    }
//
//    private void extract(MPModel input) {
//        this.input = input;
//
//        try {
//            scip = env.create();
//
//            createVariables();
//            createConstraints();
//            createObjective();
//        }
//        catch (NativeScipException e) {
//            System.err.println("Error while extracting model " + input.getName() + " to scip.");
//            e.printStackTrace();
//        }
//    }
//
//    /* Build duals and solution(s) */
//
//    private void buildSolution() throws NativeScipException {
//        buildSolution(-1);
//    }
//
//    private void buildSolution(int index) throws NativeScipException {
//        if (index < 0) {
//            objValue = env.getObjValue();
//            solution = env.getValues(vars.toArray(new JniScipVar[vars.size()]));
//
//            objValues.add(objValue);
//            solutions.add(solution);
//        }
//        else {
//            objValues.add(env.getObjValue(index));
//            solutions.add(env.getValues(vars.toArray(new JniScipVar[vars.size()]), index));
//        }
//    }
//
//    /* Creation of constraints, variables and objective function */
//
//    private void addConstraint(MPLinConstr inputConstr) throws NativeScipException {
//        long constr = envConsLinear.createConsBasicLinear(scip, inputConstr.getName(), 0, null, null, -Double.MAX_VALUE, +Double.MAX_VALUE);
//
//        for (Map.Entry<MPVar, Double> entry : inputConstr.coeffs())
//            envConsLinear.addCoefLinear(scip, constr, vars.get(entry.getKey().getIndex()), entry.getValue());
//
//        if (inputConstr.getSense() == MPLinConstr.LE || inputConstr.getSense() == MPLinConstr.EQ)
//            envConsLinear.chgLhsLinear(scip, constr, -inputConstr.getConstant());
//        if (inputConstr.getSense() == MPLinConstr.GE || inputConstr.getSense() == MPLinConstr.EQ)
//            envConsLinear.chgRhsLinear(scip, constr, -inputConstr.getConstant());
//
//        env.addCons(scip, constr);
//        constrs.add(constr);
//    }
//
//    private void addVar(MPVar inputVariable) throws NativeScipException {
//        int varType = inputVariable.getType() == MPVar.BINARY ? JniScipVartype.SCIP_VARTYPE_BINARY
//          : inputVariable.getType() == MPVar.CONTINUOUS ? JniScipVartype.SCIP_VARTYPE_CONTINUOUS
//          : inputVariable.getType() == MPVar.INTEGER ? JniScipVartype.SCIP_VARTYPE_INTEGER ? -1;
//        long var = env.createVarBasic(scip, inputVariable.getName(), inputVariable.getLB(), inputVariable.getUB(), inputVariable.getObj(), varType);
//
//        env.addVar(scip, var);
//        vars.add(var);
//
//        for (Map.Entry<MPLinConstr, Double> entry : inputVariable.coeffs())
//            if (constrs.size() > entry.getKey().getIndex())
//                envConsLinear.addCoefLinear(scip, constrs.get(entry.getKey().getIndex()), var, entry.getValue());
//    }
//
//    private void createConstraints() throws NativeScipException {
//        for (MPLinConstr inputConstr : input.contrs())
//            addConstraint(inputConstr);
//    }
//
//    private void createObjective() throws NativeScipException {
//        throw new Error("")
//    }
//
//    private void createVariables() throws NativeScipException {
//        for (MPVar inputVariable : input.vars()) {
//            if (inputVariable.getType() == MPVar.BINARY && inputVariable.getLB() == 0. && inputVariable.getUB() == 1.)
//                vars.add(env.boolVar(inputVariable.getName()));
//            else if (inputVariable.getType() == MPVar.INTEGER)
//                vars.add(env.intVar(( int ) Math.round(inputVariable.getLB()), ( int ) Math.round(inputVariable.getUB()), inputVariable.getName()));
//            else
//                vars.add(env.numVar(inputVariable.getLB(), inputVariable.getUB(), inputVariable.getName()));
//        }
//    }
//
//    // region Parameters getters and setters
//
//    private void initializeParams() {
//        booleanParams.put(BooleanParam.NumericalEmphasis, IloCplex.BooleanParam.NumericalEmphasis);
//
//        doubleParams.put(DoubleParam.CutLo, IloCplex.DoubleParam.CutLo);
//        doubleParams.put(DoubleParam.CutUp, IloCplex.DoubleParam.CutUp);
//        doubleParams.put(DoubleParam.MIPGap, IloCplex.DoubleParam.EpGap);
//        doubleParams.put(DoubleParam.MIPGapAbs, IloCplex.DoubleParam.EpAGap);
//        doubleParams.put(DoubleParam.ObjDif, IloCplex.DoubleParam.ObjDif);
//        doubleParams.put(DoubleParam.RelObjDif, IloCplex.DoubleParam.RelObjDif);
//        doubleParams.put(DoubleParam.TimeLimit, IloCplex.DoubleParam.TimeLimit);
//
//        intParams.put(IntParam.PopulateLim, IloCplex.IntParam.PopulateLim);
//        intParams.put(IntParam.RootAlg, IloCplex.IntParam.RootAlg);
//        intParams.put(IntParam.Threads, IloCplex.IntParam.Threads);
//
//        longParams.put(LongParam.IntSolLim, IloCplex.LongParam.IntSolLim);
//        longParams.put(LongParam.IterLimit, IloCplex.LongParam.ItLim);
//    }
//
//    @Override
//    public boolean getParam(BooleanParam param) {
//        try {
//            return env.getParam(booleanParams.get(param));
//        }
//        catch (NativeScipException e) {
//            System.err.println("Error while getting parameter " + param);
//            e.printStackTrace();
//        }
//        catch (NullPointerException e) {
//            System.err.println("Warning: parameter " + param + " is not supported by this solver.");
//        }
//        return false;
//    }
//
//    @Override
//    public double getParam(DoubleParam param) {
//        try {
//            return env.getParam(doubleParams.get(param));
//        }
//        catch (NativeScipException e) {
//            System.err.println("Error while getting parameter " + param);
//            e.printStackTrace();
//        }
//        catch (NullPointerException e) {
//            System.err.println("Warning: parameter " + param + " is not supported by this solver.");
//        }
//        return -1;
//    }
//
//    @Override
//    public int getParam(IntParam param) {
//        try {
//            return env.getParam(intParams.get(param));
//        }
//        catch (NativeScipException e) {
//            System.err.println("Error while getting parameter " + param);
//            e.printStackTrace();
//        }
//        catch (NullPointerException e) {
//            System.err.println("Warning: parameter " + param + " is not supported by this solver.");
//        }
//        return -1;
//    }
//
//    @Override
//    public long getParam(LongParam param) {
//        try {
//            return env.getParam(longParams.get(param));
//        }
//        catch (NativeScipException e) {
//            System.err.println("Error while getting parameter " + param);
//            e.printStackTrace();
//        }
//        catch (NullPointerException e) {
//            System.err.println("Warning: parameter " + param + " is not supported by this solver.");
//        }
//        return -1;
//    }
//
//    @Override
//    public String getParam(StringParam param) {
//        try {
//            return env.getParam(stringParams.get(param));
//        }
//        catch (NativeScipException e) {
//            System.err.println("Error while getting parameter " + param);
//            e.printStackTrace();
//        }
//        catch (NullPointerException e) {
//            System.err.println("Warning: parameter " + param + " is not supported by this solver.");
//        }
//        return null;
//    }
//
//    @Override
//    public void setParam(BooleanParam param, boolean value) {
//        try {
//            env.setParam(booleanParams.get(param), value);
//        }
//        catch (NativeScipException e) {
//            System.err.println("Error while setting parameter " + param);
//            e.printStackTrace();
//        }
//        catch (NullPointerException e) {
//            System.err.println("Warning: parameter " + param + " is not supported by this solver.");
//        }
//    }
//
//    @Override
//    public void setParam(DoubleParam param, double value) {
//        try {
//            env.setParam(doubleParams.get(param), value);
//        }
//        catch (NativeScipException e) {
//            System.err.println("Error while setting parameter " + param);
//            e.printStackTrace();
//        }
//        catch (NullPointerException e) {
//            System.err.println("Warning: parameter " + param + " is not supported by this solver.");
//        }
//    }
//
//    @Override
//    public void setParam(IntParam param, int value) {
//        try {
//            env.setParam(intParams.get(param), value);
//        }
//        catch (NativeScipException e) {
//            System.err.println("Error while setting parameter " + param);
//            e.printStackTrace();
//        }
//        catch (NullPointerException e) {
//            System.err.println("Warning: parameter " + param + " is not supported by this solver.");
//        }
//    }
//
//    @Override
//    public void setParam(LongParam param, long value) {
//        try {
//            env.setParam(longParams.get(param), value);
//        }
//        catch (NativeScipException e) {
//            System.err.println("Error while setting parameter " + param);
//            e.printStackTrace();
//        }
//        catch (NullPointerException e) {
//            System.err.println("Warning: parameter " + param + " is not supported by this solver.");
//        }
//    }
//
//    @Override
//    public void setParam(StringParam param, String value) {
//        try {
//            env.setParam(stringParams.get(param), value);
//        }
//        catch (NativeScipException e) {
//            System.err.println("Error while setting parameter " + param);
//            e.printStackTrace();
//        }
//        catch (NullPointerException e) {
//            System.err.println("Warning: parameter " + param + " is not supported by this solver.");
//        }
//    }
//
//    // endregion
//}

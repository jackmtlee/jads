package jads.mp;

import jads.mp.util.*;

import java.util.*;

/**
 * This is the main Model class, responsible for representing the formulation.
 *
 * @author Tulio Toffolo
 */
public class MPModel {

    public static final double EPS = 1e-6;

    private String name = "";
    private MPSolver solver = null;

    private List<MPLinConstr> constrs = new ArrayList<MPLinConstr>();
    private List<MPVar> variables = new ArrayList<MPVar>();
    private MPObjective objective = new MPObjective();

    private HashMap<String, MPLinConstr> constrsMap = new HashMap<String, MPLinConstr>();
    private HashMap<String, MPVar> varsMap = new HashMap<String, MPVar>();

    /**
     * The customCoeffsMap stores references to coefficients which require
     * computation, those which employ specific parameters (like "__iter__") or
     * functions (like "__sqrt__")
     */
    private HashMap<Pair<MPVar, MPLinExpr>, String> customCoeffsMap = new LinkedHashMap<Pair<MPVar, MPLinExpr>, String>();

    private List<MPAction> actions = new LinkedList<MPAction>();

    protected int intVarCount = 0;

    /**
     * Instantiates a new Model.
     */
    public MPModel() { }

    /**
     * Instantiates a new Model.
     *
     * @param direction the direction of the optimization, i.e.
     *                  Objective.MINIMIZE ('-') or Objective.MAXIMIZE ('+')
     */
    public MPModel(char direction) {
        this.objective.setDirection(direction);
    }

    /**
     * Instantiates a new Model.
     *
     * @param name the name of the model
     */
    public MPModel(String name) {
        this.name = name;
    }

    /**
     * Instantiates a new Model.
     *
     * @param direction the direction of the optimization, i.e.
     *                  Objective.MINIMIZE ('-') or Objective.MAXIMIZE ('+')
     * @param name      the name of the model
     */
    public MPModel(char direction, String name) {
        this.name = name;
        this.objective.setDirection(direction);
    }


    /**
     * Get an iterable of the changes made to the model.
     *
     * @return the iterable of the changes made to the model
     */
    public Iterable<MPAction> actions() {
        return actions;
    }

    /**
     * Adds a linear constraint to the model.
     *
     * @param lhs   the left-hand side of the constraint
     * @param sense the sense of the constraint ('&lt;', '&gt;' or '=')
     * @param rhs   the right-hand side of the constraint
     * @param name  the name  of the constraint
     * @return the linear constraint
     */
    public MPLinConstr addConstr(MPLinExpr lhs, char sense, double rhs, String name, Object... nameArgs) {
        lhs.addConstant(-rhs);
        MPLinConstr constr = new MPLinConstr(this, constrs.size(), lhs, sense, nameArgs == null ? name : String.format(name, nameArgs));
        constr.index = constrs.size();
        lhs.addConstant(rhs); // undo previous operation

        constrs.add(constr);
        constrsMap.put(constr.getName(), constr);

        addAction(new MPAction(MPAction.ActionType.AddConstr, constr));
        return constr;
    }

    /**
     * Adds a linear constraint to the model.
     *
     * @param lhs   the left-hand side of the constraint
     * @param sense the sense of the constraint ('&lt;', '&gt;' or '=')
     * @param rhs   the right-hand side of the constraint
     * @param name  the name  of the constraint
     * @return the linear constraint
     */
    public MPLinConstr addConstr(MPLinExpr lhs, char sense, MPVar rhs, String name, Object... nameArgs) {
        return addConstr(new MPLinExpr(lhs).addTerm(-1., rhs), sense, 0., name, nameArgs);
    }

    /**
     * Adds a linear constraint to the model.
     *
     * @param lhs   the left-hand side of the constraint
     * @param sense the sense of the constraint ('&lt;', '&gt;' or '=')
     * @param rhs   the right-hand side of the constraint
     * @param name  the name  of the constraint
     * @return the linear constraint
     */
    public MPLinConstr addConstr(MPLinExpr lhs, char sense, MPLinExpr rhs, String name, Object... nameArgs) {
        return addConstr(new MPLinExpr(lhs).addExpr(-1., rhs), sense, 0., name, nameArgs);
    }

    /**
     * Adds a linear constraint to the model.
     *
     * @param lhs   the left-hand side of the constraint
     * @param sense the sense of the constraint ('&lt;', '&gt;' or '=')
     * @param rhs   the right-hand side of the constraint
     * @param name  the name  of the constraint
     * @return the linear constraint
     */
    public MPLinConstr addConstr(MPVar lhs, char sense, double rhs, String name, Object... nameArgs) {
        return addConstr(new MPLinExpr(lhs), sense, rhs, name, nameArgs);
    }

    /**
     * Adds a linear constraint to the model.
     *
     * @param lhs   the left-hand side of the constraint
     * @param sense the sense of the constraint ('&lt;', '&gt;' or '=')
     * @param rhs   the right-hand side of the constraint
     * @param name  the name  of the constraint
     * @return the linear constraint
     */
    public MPLinConstr addConstr(MPVar lhs, char sense, MPVar rhs, String name, Object... nameArgs) {
        return addConstr(new MPLinExpr(lhs).addTerm(-1., rhs), sense, 0., name, nameArgs);
    }

    // region mirror methods addEq, addLe and addGe

    /**
     * Adds an equality linear constraint to the model. This method is
     * equivalent to {@link #addConstr(MPLinExpr, char sense, double, String,
     * Object...)} with sense='='.
     *
     * @param lhs  the left-hand side of the constraint
     * @param rhs  the right-hand side of the constraint
     * @param name the name  of the constraint
     * @return the linear constraint
     */
    public MPLinConstr addEq(MPLinExpr lhs, double rhs, String name, Object... nameArgs) {
        return addConstr(lhs, MPLinConstr.EQ, rhs, name, nameArgs);
    }

    /**
     * Adds an equality linear constraint to the model. This method is
     * equivalent to {@link #addConstr(MPLinExpr, char sense, MPLinExpr, String,
     * Object...)} with sense='='.
     *
     * @param lhs  the left-hand side of the constraint
     * @param rhs  the right-hand side of the constraint
     * @param name the name  of the constraint
     * @return the linear constraint
     */
    public MPLinConstr addEq(MPLinExpr lhs, MPLinExpr rhs, String name, Object... nameArgs) {
        return addConstr(lhs, MPLinConstr.EQ, rhs, name, nameArgs);
    }

    /**
     * Adds an equality linear constraint to the model. This method is
     * equivalent to {@link #addConstr(MPLinExpr, char sense, MPVar, String,
     * Object...)} with sense='='.
     *
     * @param lhs  the left-hand side of the constraint
     * @param rhs  the right-hand side of the constraint
     * @param name the name  of the constraint
     * @return the linear constraint
     */
    public MPLinConstr addEq(MPLinExpr lhs, MPVar rhs, String name, Object... nameArgs) {
        return addConstr(lhs, MPLinConstr.EQ, rhs, name, nameArgs);
    }

    /**
     * Adds an equality linear constraint to the model. This method is
     * equivalent to {@link #addConstr(MPVar, char sense, double, String,
     * Object...)} with sense='='.
     *
     * @param lhs  the left-hand side of the constraint
     * @param rhs  the right-hand side of the constraint
     * @param name the name  of the constraint
     * @return the linear constraint
     */
    public MPLinConstr addEq(MPVar lhs, double rhs, String name, Object... nameArgs) {
        return addConstr(lhs, MPLinConstr.EQ, rhs, name, nameArgs);
    }

    /**
     * Adds an equality linear constraint to the model. This method is
     * equivalent to {@link #addConstr(MPVar, char sense, MPVar, String,
     * Object...)} with sense='='.
     *
     * @param lhs  the left-hand side of the constraint
     * @param rhs  the right-hand side of the constraint
     * @param name the name  of the constraint
     * @return the linear constraint
     */
    public MPLinConstr addEq(MPVar lhs, MPVar rhs, String name, Object... nameArgs) {
        return addConstr(lhs, MPLinConstr.EQ, rhs, name, nameArgs);
    }

    /**
     * Adds a 'less than or equal' linear constraint to the model. This method
     * is equivalent to {@link #addConstr(MPLinExpr, char sense, double, String,
     * Object...)} with sense='&lt;'.
     *
     * @param lhs  the left-hand side of the constraint
     * @param rhs  the right-hand side of the constraint
     * @param name the name  of the constraint
     * @return the linear constraint
     */
    public MPLinConstr addLe(MPLinExpr lhs, double rhs, String name, Object... nameArgs) {
        return addConstr(lhs, MPLinConstr.LE, rhs, name, nameArgs);
    }

    /**
     * Adds a 'less than or equal' linear constraint to the model. This method
     * is equivalent to {@link #addConstr(MPLinExpr, char sense, MPLinExpr,
     * String, Object...)} with sense='&lt;'.
     *
     * @param lhs  the left-hand side of the constraint
     * @param rhs  the right-hand side of the constraint
     * @param name the name  of the constraint
     * @return the linear constraint
     */
    public MPLinConstr addLe(MPLinExpr lhs, MPLinExpr rhs, String name, Object... nameArgs) {
        return addConstr(lhs, MPLinConstr.LE, rhs, name, nameArgs);
    }

    /**
     * Adds a 'less than or equal' linear constraint to the model. This method
     * is equivalent to {@link #addConstr(MPLinExpr, char sense, MPVar, String,
     * Object...)} with sense='&lt;'.
     *
     * @param lhs  the left-hand side of the constraint
     * @param rhs  the right-hand side of the constraint
     * @param name the name  of the constraint
     * @return the linear constraint
     */
    public MPLinConstr addLe(MPLinExpr lhs, MPVar rhs, String name, Object... nameArgs) {
        return addConstr(lhs, MPLinConstr.LE, rhs, name, nameArgs);
    }

    /**
     * Adds a 'less than or equal' linear constraint to the model. This method
     * is equivalent to {@link #addConstr(MPVar, char sense, double, String,
     * Object...)} with sense='&lt;'.
     *
     * @param lhs  the left-hand side of the constraint
     * @param rhs  the right-hand side of the constraint
     * @param name the name  of the constraint
     * @return the linear constraint
     */
    public MPLinConstr addLe(MPVar lhs, double rhs, String name, Object... nameArgs) {
        return addConstr(lhs, MPLinConstr.LE, rhs, name, nameArgs);
    }

    /**
     * Adds a 'less than or equal' linear constraint to the model. This method
     * is equivalent to {@link #addConstr(MPVar, char sense, MPVar, String,
     * Object...)} with sense='&lt;'.
     *
     * @param lhs  the left-hand side of the constraint
     * @param rhs  the right-hand side of the constraint
     * @param name the name  of the constraint
     * @return the linear constraint
     */
    public MPLinConstr addLe(MPVar lhs, MPVar rhs, String name, Object... nameArgs) {
        return addConstr(lhs, MPLinConstr.LE, rhs, name, nameArgs);
    }

    /**
     * Adds a 'greater than or equal' linear constraint to the model. This
     * method is equivalent to {@link #addConstr(MPLinExpr, char sense, double,
     * String, Object...)} with sense='&gt;'.
     *
     * @param lhs  the left-hand side of the constraint
     * @param rhs  the right-hand side of the constraint
     * @param name the name  of the constraint
     * @return the linear constraint
     */
    public MPLinConstr addGe(MPLinExpr lhs, double rhs, String name, Object... nameArgs) {
        return addConstr(lhs, MPLinConstr.GE, rhs, name, nameArgs);
    }

    /**
     * Adds a 'greater than or equal' linear constraint to the model. This
     * method is equivalent to {@link #addConstr(MPLinExpr, char sense,
     * MPLinExpr, String, Object...)} with sense='&gt;'.
     *
     * @param lhs  the left-hand side of the constraint
     * @param rhs  the right-hand side of the constraint
     * @param name the name  of the constraint
     * @return the linear constraint
     */
    public MPLinConstr addGe(MPLinExpr lhs, MPLinExpr rhs, String name, Object... nameArgs) {
        return addConstr(lhs, MPLinConstr.GE, rhs, name, nameArgs);
    }

    /**
     * Adds a 'greater than or equal' linear constraint to the model. This
     * method is equivalent to {@link #addConstr(MPLinExpr, char sense, MPVar,
     * String, Object...)} with sense='&gt;'.
     *
     * @param lhs  the left-hand side of the constraint
     * @param rhs  the right-hand side of the constraint
     * @param name the name  of the constraint
     * @return the linear constraint
     */
    public MPLinConstr addGe(MPLinExpr lhs, MPVar rhs, String name, Object... nameArgs) {
        return addConstr(lhs, MPLinConstr.GE, rhs, name, nameArgs);
    }

    /**
     * Adds a 'greater than or equal' linear constraint to the model. This
     * method is equivalent to {@link #addConstr(MPVar, char sense, double,
     * String, Object...)} with sense='&gt;'.
     *
     * @param lhs  the left-hand side of the constraint
     * @param rhs  the right-hand side of the constraint
     * @param name the name  of the constraint
     * @return the linear constraint
     */
    public MPLinConstr addGe(MPVar lhs, double rhs, String name, Object... nameArgs) {
        return addConstr(lhs, MPLinConstr.GE, rhs, name, nameArgs);
    }

    /**
     * Adds a 'greater than or equal' linear constraint to the model. This
     * method is equivalent to {@link #addConstr(MPVar, char sense, MPVar,
     * String, Object...)} with sense='&gt;'.
     *
     * @param lhs  the left-hand side of the constraint
     * @param rhs  the right-hand side of the constraint
     * @param name the name  of the constraint
     * @return the linear constraint
     */
    public MPLinConstr addGe(MPVar lhs, MPVar rhs, String name, Object... nameArgs) {
        return addConstr(lhs, MPLinConstr.GE, rhs, name, nameArgs);
    }

    // endregion mirror methods addEq, addLe and addGe

    /**
     * Adds a parametrized coefficient for a variable in a linear expression.
     *
     * @param var      the variable whose coefficient is parametrized
     * @param expr     the linear expression
     * @param function the function responsible for computing the coefficient
     *                 value
     */
    public void addCustomCoeff(MPVar var, MPLinExpr expr, String function) {
        customCoeffsMap.put(new Pair<MPVar, MPLinExpr>(var, expr), function);
    }

    /**
     * Adds a variable to the model.
     *
     * @param lb   the lower bound of the variable
     * @param ub   the upper bound of the variable
     * @param type the type of the variable ('C' for continuous, 'I' for integer
     *             or 'B' for binary)
     * @param name the name of the variable
     * @return the created variable
     */
    public MPVar addVar(double lb, double ub, char type, String name, Object... nameArgs) {
        MPVar variable = new MPVar(this, lb, ub, type, nameArgs == null ? name : String.format(name, nameArgs));
        variable.index = variables.size();

        assert !varsMap.containsKey(variable.getName());

        variables.add(variable);
        varsMap.put(variable.getName(), variable);

        if (type == 'B' || type == 'I')
            intVarCount++;

        addAction(new MPAction(MPAction.ActionType.AddVar, variable));
        return variable;
    }

    /**
     * Adds a variable to the model.
     *
     * @param lb   the lower bound of the variable
     * @param ub   the upper bound of the variable
     * @param type the type of the variable ('C' for continuous, 'I' for integer
     *             or 'B' for binary)
     * @param obj  the coefficient of the variable in the objective function
     * @param name the name of the variable
     * @return the created variable
     */
    public MPVar addVar(double lb, double ub, double obj, char type, String name, Object... nameArgs) {
        MPVar variable = addVar(lb, ub, type, nameArgs == null ? name : String.format(name, nameArgs));
        objective.setCoeff(variable, obj);
        return variable;
    }

    /**
     * Adds a variable to the model.
     *
     * @param lb     the lower bound of the variable
     * @param ub     the upper bound of the variable
     * @param column the column of the variable, containing its coefficient in
     *               each constraint
     * @param type   the type of the variable ('C' for continuous, 'I' for
     *               integer or 'B' for binary)
     * @param name   the name of the variable
     * @return the created variable
     */
    public MPVar addVar(double lb, double ub, MPColumn column, char type, String name, Object... nameArgs) {
        MPVar variable = addVar(lb, ub, column.obj, type, nameArgs == null ? name : String.format(name, nameArgs));
        for (Map.Entry<MPLinConstr, Double> pair : column)
            pair.getKey().setCoeff(variable, pair.getValue());
        return variable;
    }

    // region mirror methods addBinVar, addIntVar, addNumVar

    /**
     * Adds a binary variable to the model. This method is equivalent to {@link
     * #addVar(double lb, double ub, char type, String, Object...)} with lb=0,
     * ub=1 and type='B'.
     *
     * @param name the name of the variable
     * @return the variable
     */
    public MPVar addBinVar(String name, Object... nameArgs) {
        return addVar(0., 1., MPVar.BINARY, name, nameArgs);
    }

    /**
     * Adds a binary variable to the model. This method is equivalent to {@link
     * #addVar(double lb, double ub, double obj, char type, String, Object...)}
     * with lb=0, ub=1 and type='B'.
     *
     * @param obj  the coefficient of the variable in the objective function
     * @param name the name of the variable
     * @return the variable
     */
    public MPVar addBinVar(double obj, String name, Object... nameArgs) {
        return addVar(0., 1., obj, MPVar.BINARY, name, nameArgs);
    }

    /**
     * Adds a binary variable to the model. This method is equivalent to {@link
     * #addVar(double lb, double ub, MPColumn column, char type, String,
     * Object...)} with lb=0, ub=1 and type='B'.
     *
     * @param column the column of the variable, containing its coefficient in
     *               each constraint
     * @param name   the name of the variable
     * @return the variable
     */
    public MPVar addBinVar(MPColumn column, String name, Object... nameArgs) {
        return addVar(0., 1., column, MPVar.BINARY, name, nameArgs);
    }

    /**
     * Adds an integer variable to the model. This method is equivalent to
     * {@link #addVar(double lb, double ub, char type, String, Object...)} with
     * type='I'.
     *
     * @param lb   the lower bound of the variable
     * @param ub   the upper bound of the variable
     * @param name the name of the variable
     * @return the variable
     */
    public MPVar addIntVar(int lb, int ub, String name, Object... nameArgs) {
        return addVar(lb, ub, MPVar.INTEGER, name, nameArgs);
    }

    /**
     * Adds an integer variable to the model. This method is equivalent to
     * {@link #addVar(double lb, double ub, double obj, char type, String,
     * Object...)} with type='I'.
     *
     * @param lb   the lower bound of the variable
     * @param ub   the upper bound of the variable
     * @param obj  the coefficient of the variable in the objective function
     * @param name the name of the variable
     * @return the variable
     */
    public MPVar addIntVar(int lb, int ub, double obj, String name, Object... nameArgs) {
        return addVar(lb, ub, obj, MPVar.INTEGER, name, nameArgs);
    }

    /**
     * Adds an integer variable to the model. This method is equivalent to
     * {@link #addVar(double lb, double ub, MPColumn column, char type, String,
     * Object...)} with type='I'.
     *
     * @param lb     the lower bound of the variable
     * @param ub     the upper bound of the variable
     * @param column the column of the variable, containing its coefficient in
     *               each constraint
     * @param name   the name of the variable
     * @return the variable
     */
    public MPVar addIntVar(int lb, int ub, MPColumn column, String name, Object... nameArgs) {
        return addVar(lb, ub, column, MPVar.INTEGER, name, nameArgs);
    }

    /**
     * Adds a continuous (double) variable to the model. This method is
     * equivalent to {@link #addVar(double lb, double ub, char type, String,
     * Object...)} with type='C'.
     *
     * @param lb   the lower bound of the variable
     * @param ub   the upper bound of the variable
     * @param name the name of the variable
     * @return the variable
     */
    public MPVar addNumVar(double lb, double ub, String name, Object... nameArgs) {
        return addVar(lb, ub, MPVar.CONTINUOUS, name, nameArgs);
    }

    /**
     * Adds a continuous (double) variable to the model. This method is
     * equivalent to {@link #addVar(double lb, double ub, double obj, char type,
     * String, Object...)} with type='C'.
     *
     * @param lb   the lower bound of the variable
     * @param ub   the upper bound of the variable
     * @param obj  the coefficient of the variable in the objective function
     * @param name the name of the variable
     * @return the variable
     */
    public MPVar addNumVar(double lb, double ub, double obj, String name, Object... nameArgs) {
        return addVar(lb, ub, obj, MPVar.CONTINUOUS, name, nameArgs);
    }

    /**
     * Adds a continuous (double) variable to the model. This method is
     * equivalent to {@link #addVar(double lb, double ub, MPColumn column, char
     * type, String, Object...)} with type='C'.
     *
     * @param lb     the lower bound of the variable
     * @param ub     the upper bound of the variable
     * @param column the column of the variable, containing its coefficient in
     *               each constraint
     * @param name   the name of the variable
     * @return the variable
     */
    public MPVar addNumVar(double lb, double ub, MPColumn column, String name, Object... nameArgs) {
        return addVar(lb, ub, column, MPVar.CONTINUOUS, name, nameArgs);
    }

    // endregion mirror methods addBoolVar, addIntVar, addVar

    /**
     * Gets an iterable of the constraints of the model.
     *
     * @return an iterable of the constraints of the model
     */
    public Iterable<MPLinConstr> contrs() {
        return constrs;
    }

    /**
     * Removes (deletes) a constraint from the model.
     *
     * @param constr the constraint to be removed.
     * @return true if the constraint is successfully removed and false
     * otherwise
     */
    public boolean delete(MPLinConstr constr) {
        for (int i = constr.index; i < constrs.size() - 1; i++) {
            constrs.set(i, constrs.get(i + 1));
            constrs.get(i).index = i;
        }
        constrs.remove(constrs.size() - 1);
        constrsMap.remove(constr.getName());

        addAction(new MPAction(MPAction.ActionType.DelConstr, constr));
        return true;
    }

    /**
     * Removes (deletes) a variable from the model.
     *
     * @param variable the variable
     * @return true if the constraint is successfully removed and false
     * otherwise
     */
    public boolean delete(MPVar variable) {
        // removing variable from objective and all constraints
        objective.removeVar(variable);
        MPLinConstr constrs[] = variable.getColumn().keySet().toArray(new MPLinConstr[variable.getColumn().keySet().size()]);
        for (MPLinConstr c : constrs)
            c.removeVar(variable);

        // deleting variable from list of variables
        for (int i = variable.index; i < variables.size() - 1; i++) {
            variables.set(i, variables.get(i + 1));
            variables.get(i).index = i;
        }
        variables.remove(variables.size() - 1);
        varsMap.remove(variable.getName());

        // updating counter of integer variables
        if (variable.getType() == 'B' || variable.getType() == 'I')
            intVarCount--;

        // creating action to update solvers
        addAction(new MPAction(MPAction.ActionType.DelVar, variable));

        return true;
    }

    /**
     * Gets if the model has any integer variable, i.e. whether it is not
     * continuous.
     *
     * @return if the model has any integer variable, i.e. whether it is not
     * continuous.
     */
    public boolean hasIntVar() {
        return intVarCount > 0;
    }

    /**
     * Gets the constraint with a certain index.
     *
     * @param index the index of the constraint
     * @return the constraint at position {@code index}
     */
    public MPLinConstr getConstr(int index) {
        return constrs.get(index);
    }

    /**
     * Gets the constraint with a certain name.
     *
     * @param constrName the name of the constraint
     * @return the constraint that has {@code constrName}
     */
    public MPLinConstr getConstr(String constrName) {
        return constrsMap.get(constrName);
    }

    /**
     * Gets the number of constraints in the model.
     *
     * @return the number of constraints in the model
     */
    public int getNConstrs() {
        return constrs.size();
    }

    /**
     * Gets the number of variables in the model.
     *
     * @return the number of variables in the model
     */
    public int getNVars() {
        return variables.size();
    }

    /**
     * Gets the objective function object.
     *
     * @return the objective function
     */
    public MPObjective getObjective() {
        return objective;
    }

    /**
     * Sets the objective function.
     *
     * @param objective the new objective function to be considered
     */
    public void setObjective(MPObjective objective) {
        this.objective = objective;
    }

    /**
     * Gets the name of the model.
     *
     * @return the name of the model
     */
    public String getName() {
        return name;
    }

    /**
     * Sets the name of the model.
     *
     * @param name the new name for the model.
     * @return the name of the model
     */
    public void setName(String name) {
        this.name = name;
    }

    /**
     * Gets the current solver of the model.
     *
     * @return the solver
     */
    public MPSolver getSolver() {
        return solver;
    }

    /**
     * Sets the solver to be used.
     *
     * @param solver the solver
     */
    public void setSolver(MPSolver solver) {
        this.solver = solver;
        actions.clear();
    }

    /**
     * * Gets the variable with a certain index.
     *
     * @param index the index of the variable
     * @return the variable at position {@code index}
     */
    public MPVar getVar(int index) {
        return variables.get(index);
    }

    /**
     * Gets the variable with a certain name.
     *
     * @param varName the name of the variable
     * @return the variable that has {@code varName}
     */
    public MPVar getVar(String varName) {
        return varsMap.get(varName);
    }

    /**
     * Updates the solver with the latest modifications in the model.
     */
    public void updateSolver() {
        solver.updateModel();
        solver.updateObjective();
        actions.clear();
    }

    /**
     * Gets an iterable of the variables of the model.
     *
     * @return the iterable of the variables of the model
     */
    public Iterable<MPVar> vars() {
        return variables;
    }


    /**
     * Adds an action.
     *
     * @param action the action
     */
    protected void addAction(MPAction action) {
        if (solver != null) actions.add(action);
    }
}

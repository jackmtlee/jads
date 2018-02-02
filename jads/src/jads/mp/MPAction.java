package jads.mp;

/**
 * This class represents actions that must be performed within the solvers, i.e.
 * updates that were not yet committed. Its main goal is to improve performance
 * while changing the formulation.
 *
 * @author Tulio Toffolo
 */
public class MPAction {

    public final ActionType type;
    public final double coeff;
    public final MPLinConstr constr;
    public final MPVar variable;


    /**
     * Instantiates a new Action.
     *
     * @param type     the type of the action
     * @param variable the variable related to the action
     */
    protected MPAction(ActionType type, MPVar variable) {
        assert type == ActionType.AddVar || type == ActionType.DelVar || type == ActionType.SetVarBounds || type == ActionType.SetVarType;

        this.type = type;
        this.coeff = 0.;
        this.constr = null;
        this.variable = variable;
    }

    /**
     * Instantiates a new Action.
     *
     * @param type   the type of the action
     * @param constr the constraint related to the action
     */
    protected MPAction(ActionType type, MPLinConstr constr) {
        assert type == ActionType.AddConstr || type == ActionType.DelConstr;

        this.type = type;
        this.coeff = 0.;
        this.constr = constr;
        this.variable = null;
    }

    /**
     * Instantiates a new Action.
     *
     * @param type     the type of the action
     * @param coeff    the new coefficient value
     * @param constr   the related constraint (row)
     * @param variable the related variable (column)
     */
    protected MPAction(ActionType type, double coeff, MPLinConstr constr, MPVar variable) {
        assert type == ActionType.EditConstr;

        this.type = type;
        this.coeff = coeff;
        this.constr = constr;
        this.variable = variable;
    }


    /**
     * Enum with the possible action types.
     */
    public enum ActionType {
        AddConstr, AddVar, EditConstr, DelConstr, DelVar,
        SetVarBounds, SetVarType,
    }
}

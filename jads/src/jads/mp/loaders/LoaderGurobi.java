package jads.mp.loaders;

import gurobi.*;
import jads.mp.*;

/**
 * This class provides a means of loading formulation from files using Gurobi.
 *
 * @author Tulio Toffolo
 */
public class LoaderGurobi implements MPLoader {

    @Override
    public MPModel loadModel(String filePath) {
        System.out.println("Loading (with Gurobi) file " + filePath + "...");
        MPModel model;

        try {
            GRBEnv env = new GRBEnv();
            env.set(GRB.StringParam.LogFile, "");
            env.set(GRB.IntParam.LogToConsole, 0);

            GRBModel gurobi = new GRBModel(env, filePath);

            GRBVar[] vars = gurobi.getVars();
            GRBConstr[] cons = gurobi.getConstrs();
            GRBLinExpr obj = ( GRBLinExpr ) gurobi.getObjective();

            model = new MPModel(gurobi.get(GRB.StringAttr.ModelName));

            for (GRBVar var : vars)
                model.addVar(var.get(GRB.DoubleAttr.LB), var.get(GRB.DoubleAttr.UB), var.get(GRB.CharAttr.VType), var.get(GRB.StringAttr.VarName));

            MPObjective objective = model.getObjective();
            objective.setDirection(gurobi.get(GRB.IntAttr.ModelSense) == 1 ? MPObjective.MINIMIZE : MPObjective.MAXIMIZE);

            for (int v = 0; v < obj.size(); v++) {
                objective.setCoeff(model.getVar(obj.getVar(v).get(GRB.StringAttr.VarName)), obj.getCoeff(v));
            }

            for (GRBConstr con : cons) {
                MPLinExpr expr = new MPLinExpr();

                GRBLinExpr row = gurobi.getRow(con);
                for (int v = 0; v < row.size(); v++) {
                    expr.addTerm(row.getCoeff(v), model.getVar(row.getVar(v).get(GRB.StringAttr.VarName)));
                }

                if (con.get(GRB.CharAttr.Sense) == '=')
                    model.addConstr(expr, MPLinConstr.EQ, con.get(GRB.DoubleAttr.RHS), con.get(GRB.StringAttr.ConstrName));

                else if (con.get(GRB.CharAttr.Sense) == '<')
                    model.addConstr(expr, MPLinConstr.LE, con.get(GRB.DoubleAttr.RHS), con.get(GRB.StringAttr.ConstrName));

                else if (con.get(GRB.CharAttr.Sense) == '>')
                    model.addConstr(expr, MPLinConstr.GE, con.get(GRB.DoubleAttr.RHS), con.get(GRB.StringAttr.ConstrName));

                else {
                    System.err.printf("\nRanged constraints like %s are currently not supported.\n", con.get(GRB.StringAttr.ConstrName));
                    System.exit(-1);
                }
            }
        }
        catch (GRBException e) {
            System.err.println("Error while reading input model (with Gurobi).");
            e.printStackTrace();
            return null;
        }

        System.out.println("There are " + model.getNVars() + " variables and " + model.getNConstrs() + " constraints.");
        System.out.println();

        return model;
    }
}

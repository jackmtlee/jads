package jads.mp.loaders;

import ilog.concert.*;
import ilog.cplex.*;
import jads.mp.*;

/**
 * This class provides a means of loading formulation from files using Cplex.
 *
 * @author Tulio Toffolo
 */
public class LoaderCplex implements MPLoader {

    @Override
    public MPModel loadModel(String filePath) {
        System.out.println("Loading (with Cplex) file " + filePath + "...");
        MPModel model;

        try {
            IloCplex cplex = new IloCplex();
            cplex.setOut(null);
            cplex.importModel(filePath);

            IloNumVar[] vars = (( IloLPMatrix ) cplex.LPMatrixIterator().next()).getNumVars();
            IloRange[] cons = (( IloLPMatrix ) cplex.LPMatrixIterator().next()).getRanges();
            IloObjective obj = cplex.getObjective();

            model = new MPModel(cplex.getName());

            for (IloNumVar var : vars) {
                if (var.getType() == IloNumVarType.Bool)
                    model.addVar(var.getLB(), var.getUB(), MPVar.BINARY, var.getName());
                else if (var.getType() == IloNumVarType.Int)
                    model.addVar(var.getLB(), var.getUB(), MPVar.INTEGER, var.getName());
                else if (var.getType() == IloNumVarType.Float)
                    model.addVar(var.getLB(), var.getUB(), MPVar.CONTINUOUS, var.getName());
                else
                    throw new IloException("invalid variable type for variable " + var.getName());
            }

            MPObjective objective = model.getObjective();
            objective.setDirection(obj.getSense() == IloObjectiveSense.Minimize ? MPObjective.MINIMIZE : MPObjective.MAXIMIZE);
            IloLinearNumExprIterator iterator = (( IloLinearNumExpr ) obj.getExpr()).linearIterator();
            while (iterator.hasNext()) {
                IloNumVar var = iterator.nextNumVar();
                objective.setCoeff(model.getVar(var.getName()), iterator.getValue());
            }

            for (IloRange con : cons) {
                MPLinExpr expr = new MPLinExpr();
                iterator = (( IloLinearNumExpr ) con.getExpr()).linearIterator();
                while (iterator.hasNext()) {
                    IloNumVar var = iterator.nextNumVar();
                    expr.addTerm(iterator.getValue(), model.getVar(var.getName()));
                }

                if (Math.abs(con.getLB() - con.getUB()) <= MPModel.EPS)
                    model.addConstr(expr, MPLinConstr.EQ, con.getUB(), con.getName());

                else if (con.getLB() <= Double.MIN_VALUE)
                    model.addConstr(expr, MPLinConstr.LE, con.getUB(), con.getName());

                else if (con.getUB() >= Double.MAX_VALUE)
                    model.addConstr(expr, MPLinConstr.GE, con.getLB(), con.getName());

                else {
                    System.err.printf("\nRanged constraints like %s are currently not supported.\n", con.getName());
                    System.exit(-1);
                }
            }
        }
        catch (IloException e) {
            System.err.println("Error while reading input model (with Cplex).");
            e.printStackTrace();
            return null;
        }

        System.out.println("There are " + model.getNVars() + " variables and " + model.getNConstrs() + " constraints.");
        System.out.println();

        return model;
    }
}

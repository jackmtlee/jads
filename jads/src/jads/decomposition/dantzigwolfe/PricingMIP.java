package jads.decomposition.dantzigwolfe;

import jads.mp.*;

import java.util.*;

public class PricingMIP extends Pricing {

    private int nRuns = 0;
    private MPModel model;
    private MPSolver solver;

    public PricingMIP(Block block) {
        super(block);
        model = new MPModel(data.model.getObjective().getDirection(), data.name + "-pricing-" + block.index);

        createVariables();
        createConstraints();

        solver = params.getNewSolver(model);
        model.setSolver(solver);
        assert solver != null;

        if (model.getObjective().getDirection() == MPObjective.MINIMIZE)
            solver.setParam(MPSolver.DoubleParam.CutUp, -Parameters.EPS);
        else
            solver.setParam(MPSolver.DoubleParam.CutLo, +Parameters.EPS);
    }

    public boolean run(double redCosts[], double fixedCost) {
        updateObjective(redCosts, fixedCost);

        objValues.clear();
        solutions.clear();

        if (params.pricing.solLimit != Integer.MAX_VALUE)
            solver.setParam(MPSolver.LongParam.IntSolLim, params.pricing.solLimit);

        //solver.writeModel("log/" + model.name + ".lp");
        nRuns++;

        return (params.pricing.populate && populate()) || optimize();
    }

    private boolean optimize() {
        if (solver.solve()) {
            if (((model.getObjective().getDirection() == MPObjective.MINIMIZE) && (solver.getObjValue() < -Parameters.EPS))
              || ((model.getObjective().getDirection() == MPObjective.MAXIMIZE) && (solver.getObjValue() > Parameters.EPS))) {
                objValues.add(solver.getObjValue());
                solutions.add(solver.getSolution());
            }
        }
        return !solutions.isEmpty();
    }

    private boolean populate() {
        solver.setParam(MPSolver.IntParam.PopulateLim, params.pricing.numPopulate);

        if (solver.populate()) {
            for (int s = 0; s < solver.getSolutions().size(); s++) {
                if (((model.getObjective().getDirection() == MPObjective.MINIMIZE) && (solver.getObjValue() < -Parameters.EPS))
                  || ((model.getObjective().getDirection() == MPObjective.MAXIMIZE) && (solver.getObjValue() > Parameters.EPS))) {
                    objValues.add(solver.getObjValues().get(s));
                    solutions.add(solver.getSolutions().get(s));
                }
            }
        }
        return !solutions.isEmpty();
    }

    private void createConstraints() {
        for (MPLinConstr originalConstr : block.constraints) {
            MPLinExpr lhs = new MPLinExpr(originalConstr.getConstant());

            for (Map.Entry<MPVar, Double> entry : originalConstr.coeffs())
                if (block.varsIndexes.containsKey(entry.getKey().getIndex()))
                    lhs.addTerm(model.getVar(block.varsIndexes.get(entry.getKey().getIndex())), entry.getValue());

            model.addConstr(lhs, originalConstr.getSense(), 0, originalConstr.getName());
        }
    }

    private void createVariables() {
        for (int v = 0; v < block.variables.size(); v++) {
            MPVar originalVar = data.model.getVar(block.variables.get(v).getIndex());
            model.addVar(originalVar.getLB(), originalVar.getUB(), originalVar.getObj(), originalVar.getType(), originalVar.getName());
        }
    }

    private void updateObjective(double redCosts[], double fixedCost) {
        model.getObjective().clear();
        model.getObjective().setConstant(fixedCost);
        for (int v = 0; v < redCosts.length; v++)
            if (redCosts[v] != 0.0)
                model.getObjective().addTerm(model.getVar(v), redCosts[v]);
        model.updateSolver();
    }
}

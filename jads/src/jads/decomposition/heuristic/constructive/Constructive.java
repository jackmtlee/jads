package jads.decomposition.heuristic.constructive;

import jads.decomposition.heuristic.*;
import jads.mp.*;

import java.util.*;

/**
 * This class implements a constructive decomposition-based algorithm.
 *
 * @author Tulio Toffolo
 */
public class Constructive {

    private final Data data = Data.getInstance();
    private final Data params = Data.getInstance();
    private final Decomposition dec;

    private int eta = 4, step = 2;

    private int blockSolved[];

    public Constructive(int eta, int step) {
        this(Data.getInstance().decompositions.get(0), eta, step);
    }

    public Constructive(Decomposition decomposition, int eta, int step) {
        if (step > eta)
            throw new Error("Parameter step must be less than or equal to eta");

        this.dec = decomposition;
        this.eta = eta;
        this.step = step;
    }

    public Solution completeSolution(MPModel model, Solution solution) {
        if (solution == null) return solution;

        // initializing original lb and ub arrays
        double lb[] = new double[model.getNVars()];
        double ub[] = new double[model.getNVars()];
        for (MPVar var : model.vars()) {
            lb[var.getIndex()] = var.getLB();
            ub[var.getIndex()] = var.getUB();
            if (solution.hasValue(var.getIndex()))
                var.setBounds(solution.getValue(var.getIndex()), solution.getValue(var.getIndex()));
        }

        if (model.getSolver() == null) {
            MPSolver solver = params.getNewSolver(model);
            model.setSolver(solver);
        }
        model.updateSolver();

        if (model.getSolver().solve()) {
            solution.update(model.getSolver().getSolution());
        }
        else {
            model.getSolver().writeModel("infeasible.lp");
            solution = null;
        }

        // restoring model to its initial state
        for (MPVar var : model.vars())
            var.setBounds(lb[var.getIndex()], ub[var.getIndex()]);
        model.updateSolver();

        return solution;
    }

    public Solution solve(MPModel model) {
        // marking all blocks as 'unsolved'
        blockSolved = new int[dec.blocks.size()];

        Solution solution = solve(new Solution(model), 0);
        return completeSolution(model, solution);
    }

    private void addSolution(Solution solution, MPModel submodel) {
        List<MPVar> vars = new ArrayList<>();
        List<Double> vals = new ArrayList<>();
        // fixing variables in the model
        for (MPVar var : submodel.vars()) {
            Double x = solution.getValue(var.getName());
            if (x != null) {
                vars.add(var);
                vals.add(x);
            }
        }

        // adding (partial) initial solution to model
        MPVar varArray[] = vars.toArray(new MPVar[vars.size()]);
        double valArray[] = new double[vals.size()];
        for (int i = 0; i < valArray.length; i++) valArray[i] = vals.get(i);
        submodel.getSolver().addSolution(varArray, valArray);
    }

    private Solution solve(Solution solution, int index) {
        if (index >= dec.blocks.size())
            return solution;

        Subproblem subproblem = new Subproblem(dec, solution, dec.blocks.get(index), eta);
        System.out.printf("Solving %d blocks (reference block: %d)...\n", subproblem.size(), index);

        MPModel subproblemModel = subproblem.createModel();
        MPSolver solver = params.getNewSolver(subproblemModel);
        subproblemModel.setSolver(solver);
        solver.setParam(MPSolver.IntParam.PopulateLim, params.subproblem.solLimit);

        // adding solution to speed-up constructive algorithm
        addSolution(solution, subproblemModel);

        if (solver.solve()) {
            List<double[]> xList = solver.getSolutions();
            subproblemModel = null;
            solver = null;

            for (Block block : subproblem.getBlocks())
                blockSolved[block.getIndex()]++;

            for (double[] x : xList) {
                Solution newSolution = new Solution(solution);
                newSolution.update(subproblem.getSubproblemVars(), x);

                int j = index + 1;
                while (j < dec.blocks.size() && j < index + step && blockSolved[j] > 0)
                    j++;

                Solution finalSolution = solve(newSolution, j);
                if (finalSolution != null) return finalSolution;
            }

            for (Block block : subproblem.getBlocks())
                blockSolved[block.getIndex()]--;
        }
        else {
            System.out.println("Infeasible model...");
        }

        return null;
    }
}

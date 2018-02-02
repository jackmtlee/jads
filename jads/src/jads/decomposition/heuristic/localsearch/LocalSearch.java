package jads.decomposition.heuristic.localsearch;

import jads.decomposition.heuristic.*;
import jads.mp.*;

import java.util.*;

/**
 * This class is an straightforward implementation of the decomposition-based
 * local search algorithm described by Tulio Toffolo within his PhD thesis.
 *
 * @author Tulio Toffolo
 */
public class LocalSearch {

    private final Data data = Data.getInstance();
    private final Data params = Data.getInstance();

    private MPModel model;
    private double lb[], ub[];

    private int eta, step;
    private int etas[], steps[];
    private boolean reoptimize;

    public LocalSearch(MPModel model, int eta, int step, boolean reoptimize) {
        this.model = model;
        this.eta = eta;
        this.step = step;
        this.reoptimize = reoptimize;

        // initializing original lb and ub arrays
        lb = new double[model.getNVars()];
        ub = new double[model.getNVars()];
        for (MPVar var : model.vars()) {
            lb[var.getIndex()] = var.getLB();
            ub[var.getIndex()] = var.getUB();
        }
    }


    public Solution solve(Solution solution, long maxTimeLimitMillis) {
        System.out.printf("Initializing local search phase...\n");
        List<Decomposition> decompositions = new ArrayList<>(data.decompositions);

        MPSolver solver = model.getSolver();
        if (solver == null) {
            solver = params.getNewSolver(model);
            model.setSolver(solver);
        }

        long startTimeMillis = System.currentTimeMillis();

        etas = new int[decompositions.size()];
        steps = new int[decompositions.size()];
        for (int i = 0; i < decompositions.size(); i++) {
            etas[i] = decompositions.get(i).eta != 0 ? decompositions.get(i).eta : Math.min(this.eta, decompositions.get(i).maxEta);
            steps[i] = decompositions.get(i).step != 0 ? decompositions.get(i).step : Math.min(this.step, decompositions.get(i).maxStep);
        }

        List<Subproblem> subproblems = makeSubproblems(decompositions);
        boolean stop = false;

        while (!stop) {
            double deltaGlobal = 0;
            int i = 0, l = subproblems.size() - 1;
            while (i != l && !stop) {
                Subproblem subproblem = subproblems.get(i);
                System.out.printf("%-8s Solving %d%s blocks (reference block: %d)...\n",
                  String.format("%.1fs", (System.currentTimeMillis() - startTimeMillis) / 1000.0),
                  subproblem.size(), subproblem.dec.name.isEmpty() ? "" : " '" + subproblem.dec.name + "'", subproblem.getBlocks().get(0).getIndex());
                subproblem.updateModel(model, lb, ub, solution);
                solver.addSolution(solution.getX());

                if (solver.solve()) {
                    // update solution
                    double deltaCost = subproblem.updateSolution(model, solution, solver.getSolution());
                    if ((model.getObjective().getDirection() == MPObjective.MINIMIZE && deltaCost < 0)
                      || (model.getObjective().getDirection() == MPObjective.MAXIMIZE && deltaCost > 0)) {
                        System.out.println("          ---> solution cost has improved by " + deltaCost + " to " + solver.getObjValue());

                        deltaGlobal += deltaCost;
                    }
                }
                else {
                    System.out.println("Solver did not end correctly....");
                }

                if (System.currentTimeMillis() >= maxTimeLimitMillis) {
                    System.out.println("Runtime limit reached...");
                    return solution;
                }

                if (reoptimize && deltaGlobal < 0) {
                    deltaGlobal = 0;
                    l = i;
                }
                i = (i + 1) % subproblems.size();
            }

            if (updateParameters(decompositions) || deltaGlobal < 0) {
                subproblems = makeSubproblems(decompositions);
            }
            else {
                break;
            }
        }

        return solution;
    }

    private List<Subproblem> makeSubproblems(List<Decomposition> decompositions) {
        List<Subproblem> subproblemList = new ArrayList<>();
        for (Decomposition dec : decompositions) {
            if (dec.shuffle) {
                Collections.shuffle(dec.blocks);
                for (int i = 0; i < dec.blocks.size(); i++) {
                    dec.blocks.get(i).setIndex(i);
                }
            }

            boolean blockSolved[] = new boolean[dec.blocks.size()];
            int nBlocksSolved = 0;
            int index = 0;

            int maxBlocks = dec.blocks.size();
            if (etas[dec.getIndex()] < steps[dec.getIndex()]) {
                maxBlocks = dec.blocks.size() / etas[dec.getIndex()];
            }

            while (nBlocksSolved < maxBlocks) {
                Subproblem subproblem = new Subproblem(dec, dec.blocks.get(index), etas[dec.getIndex()]);
                subproblemList.add(subproblem);

                // marking subproblems as solved
                for (Block block : subproblem.getBlocks()) {
                    if (!blockSolved[block.getIndex()]) {
                        blockSolved[block.getIndex()] = true;
                        nBlocksSolved++;
                    }
                }

                // updating pointer 'index' by skipping at most 'step' subproblems
                //if (maxBlocks == dec.blocks.size()) {
                for (int k = 0; k < steps[dec.getIndex()]; k++) {
                    index = (index + 1) % dec.blocks.size();
                    if (!blockSolved[index]) break;
                }
                //}
                //else {
                //    index = (index + steps[dec.getIndex()]) % dec.blocks.size();
                //}
            }
        }
        Collections.shuffle(subproblemList, data.random);
        subproblemList.sort(Comparator.comparingInt(Subproblem::getPriority));
        return subproblemList;
    }

    private boolean updateParameters(List<Decomposition> decompositions) {
        boolean result = false;

        for (Decomposition dec : decompositions) {
            if (etas[dec.getIndex()] + dec.etaSkip <= dec.maxEta && etas[dec.getIndex()] + dec.etaSkip <= dec.blocks.size()) {
                etas[dec.getIndex()] += dec.etaSkip;
                steps[dec.getIndex()] = ( int ) Math.min(dec.maxStep, Math.max(Math.ceil(etas[dec.getIndex()] / 2.0), steps[dec.getIndex()]));
                result |= true;
            }
        }
        return result;
    }
}

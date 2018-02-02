package jads.decomposition.heuristic.localsearch;

import jads.decomposition.heuristic.*;
import jads.mp.*;
import jads.mp.util.*;

import java.util.*;

/**
 * This class represents a Subproblem of the local search decomposition-based
 * algorithm implemented.
 *
 * @author Tulio Toffolo
 */
public class Subproblem {

    public final Data params;
    public final Data data;
    public final Decomposition dec;

    private int index, eta;

    private boolean hasVar[];

    private List<Block> blocks = new ArrayList<>();
    private List<MPVar> originalVars = new ArrayList<>();

    private double initialCost;
    private int priority;


    public Subproblem(Decomposition decomposition, Block block, int eta) {
        this.dec = decomposition;
        this.params = Data.getInstance();
        this.data = Data.getInstance();

        this.index = index;
        this.eta = eta;

        selectBlocks(block);
    }

    public List<Block> getBlocks() {
        return blocks;
    }

    public int getPriority() {
        return priority;
    }

    public List<MPVar> getVars() {
        return originalVars;
    }

    public int size() {
        return blocks.size();
    }

    public void updateModel(MPModel model, double lb[], double ub[], Solution solution) {
        List<MPVar> vars = new ArrayList<>();
        List<Double> vals = new ArrayList<>();

        // fixing variables in the model
        for (MPVar var : model.vars()) {
            Double x = solution.getValue(var.getIndex());
            if (x != null) {
                vars.add(var);
                vals.add(x);
            }

            if (hasVar[var.getIndex()]) {
                var.setBounds(lb[var.getIndex()], ub[var.getIndex()]);
            }
            else if (dec.varsBlocks.get(var.getIndex()).isEmpty()) {
                var.setBounds(lb[var.getIndex()], ub[var.getIndex()]);
                hasVar[var.getIndex()] = true;
                originalVars.add(var);
            }
            else if (x != null) {
                var.setBounds(x, x);
            }
        }

        // updating solver
        model.updateSolver();

    }

    public double updateSolution(MPModel model, Solution solution, double x[]) {
        double deltaCost = 0;
        for (MPVar var : originalVars) {
            if (x[var.getIndex()] != solution.getValue(var.getIndex())) {
                deltaCost += var.getObj() * x[var.getIndex()];
                deltaCost -= var.getObj() * solution.getValue(var.getIndex());
                solution.setValue(var, x[var.getIndex()]);
            }
        }

        return deltaCost;
    }


    private void selectBlocks(Block block) {
        // shuffling and sorting connections
        block.sortConnections(data.random);

        blocks.clear();
        originalVars.clear();
        boolean hasBlock[] = new boolean[dec.blocks.size()];
        hasVar = new boolean[data.model.getNVars()];
        //priority = block.priority;

        PriorityQueue<PairInt<Block>> connections = new PriorityQueue<>();

        while (block != null && blocks.size() < eta) {
            if (!hasBlock[block.getIndex()]) {
                hasBlock[block.getIndex()] = true;
                //priority = Math.min(priority, block.priority);

                blocks.add(block);
                block.getConnections().forEach(connections::add);
                for (MPVar var : block.vars()) {
                    if (!hasVar[var.getIndex()]) {
                        hasVar[var.getIndex()] = true;
                        originalVars.add(var);
                    }
                }
            }

            block = connections.isEmpty() ? null : connections.poll().second;
        }
    }
}

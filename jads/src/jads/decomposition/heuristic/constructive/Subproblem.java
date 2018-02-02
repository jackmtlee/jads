package jads.decomposition.heuristic.constructive;

import jads.decomposition.heuristic.*;
import jads.mp.*;
import jads.mp.util.*;

import java.util.*;

/**
 * This class represents a Subproblem of the constructive decomposition-based algorithm.
 *
 * @author Tulio Toffolo
 */
public class Subproblem {

    private static String uuid = UUID.randomUUID().toString().replace("-", "");

    public final String name;
    public final Solution solution;

    private final Data params;
    private final Data data;
    private final Decomposition dec;

    private int index, eta;

    private List<Block> blocks = new ArrayList<>();
    private List<MPVar> subproblemVars = new ArrayList<>();


    public Subproblem(Decomposition decomposition, Solution solution, Block block, int eta) {
        this.params = Data.getInstance();
        this.data = Data.getInstance();
        this.dec = decomposition;

        this.name = String.format("subproblem(%d,%d)", block.getIndex(), eta);
        this.solution = solution;
        this.index = index;
        this.eta = eta;

        selectBlocks(block);
    }

    public List<Block> getBlocks() {
        return blocks;
    }

    public MPModel createModel() {
        MPModel model = new MPModel(name);

        // adding variables within model
        for (Block block : blocks) {
            for (MPVar blockVar : block.vars()) {
                if (model.getVar(blockVar.getName()) == null) {

                    // selecting variable from submodel (if available)
                    MPVar var = dec.submodel.getVar(blockVar.getName());
                    if (var == null) var = blockVar;

                    MPVar newVar = model.addVar(var.getLB(), var.getUB(), var.getObj(), var.getType(), var.getName());
                    subproblemVars.add(blockVar);
                }
            }
        }

        // adding constraints
        for (Block block : blocks) {
            for (MPLinConstr blockConstr : block.constrs()) {
                if (model.getConstr(blockConstr.getName()) == null) {

                    // selecting constraint from submodel (if available)
                    MPLinConstr constr = dec.submodel.getConstr(blockConstr.getName());
                    if (constr == null) constr = blockConstr;

                    MPLinExpr expr = new MPLinExpr(constr.getConstant());
                    for (Map.Entry<MPVar, Double> entry : constr.coeffs()) {
                        String varName = entry.getKey().getName();
                        MPVar var = model.getVar(varName);

                        // if variable is not in subproblem model (note it may be a slack or auxiliary one...)
                        if (var == null) {

                            MPVar originalVar = data.model.getVar(varName);
                            MPVar submodelVar = dec.submodel.getVar(varName);

                            // if variable belongs to the original problem
                            if (originalVar != null) {

                                // if variable is an auxiliary one
                                if (dec.varsBlocks.get(originalVar.getIndex()).size() == 0) {
                                    var = model.addVar(originalVar.getLB(), originalVar.getUB(), originalVar.getObj(), originalVar.getType(), originalVar.getName());
                                    //slackAuxVars.add(originalVar);
                                }
                                // else, using value rather than the variable
                                else {
                                    var = null;
                                }
                            }
                            else if (submodelVar != null) {
                                var = model.addVar(submodelVar.getLB(), submodelVar.getUB(), submodelVar.getObj(), submodelVar.getType(), submodelVar.getName());
                                //slackAuxVars.add(null);
                            }
                        }

                        // if var remains null, check for variable value and add it to formulation as a constant
                        if (var == null) {
                            if (solution.hasVar(varName))
                                expr.addConstant(entry.getValue() * solution.getValue(varName));
                        }
                        else {
                            expr.addTerm(entry.getValue(), var);
                        }
                    }

                    model.addConstr(expr, constr.getSense(), 0, constr.getName());
                }
            }
        }
        return model;
    }

    public int size() {
        return blocks.size();
    }

    public List<MPVar> getSubproblemVars() {
        return subproblemVars;
    }


    private void selectBlocks(Block block) {
        boolean hasBlock[] = new boolean[dec.blocks.size()];

        PriorityQueue<PairInt<Block>> connections = new PriorityQueue<>();

        while (block != null && blocks.size() < eta) {
            if (!hasBlock[block.getIndex()]) {
                hasBlock[block.getIndex()] = true;
                blocks.add(block);
                block.getConnections().forEach(connections::add);
            }

            block = connections.isEmpty() ? null : connections.poll().second;
        }
    }
}

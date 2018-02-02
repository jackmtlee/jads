package jads.decomposition.dantzigwolfe;

import jads.mp.*;

import java.util.*;

public class ColumnGeneration extends Algorithm {

    public MPModel model;
    public List<Block> blocks = new ArrayList<Block>();
    public List<List<MPVar>> varsBlocks = new ArrayList<List<MPVar>>();
    public List<Integer> consBlocks = new ArrayList<Integer>();
    public String name = "";

    MPSolver solver;

    ArrayList<MPVar> variables = new ArrayList<MPVar>();
    ArrayList<MPVar> artificials = new ArrayList<MPVar>();
    ArrayList<MPVar> lambdas = new ArrayList<MPVar>();

    HashMap<Integer, List<DoublePair<Integer>>> varsCoeffs = new HashMap<Integer, List<DoublePair<Integer>>>();
    ArrayList<HashMap<Integer, ArrayList<DoublePair<Integer>>>> linkingCoeffs = new ArrayList<HashMap<Integer, ArrayList<DoublePair<Integer>>>>();

    public ColumnGeneration() {
        this(new MPModel("master"));
    }

    public ColumnGeneration(MPModel model) {
        this.model = model;
        this.model.setName(data.name + "-master");

        createVariables();
        this.model.getObjective().setDirection(data.model.getObjective().getDirection());
        createConstraints();

        solver = params.getNewSolver(model);
        model.setSolver(solver);
    }

    public boolean solve() {
        long initialTime = System.currentTimeMillis();

        //loadColumnsFile();

        //model.setParam(Solver.BooleanParam.NumericalEmphasis, true);
        //model.setParam(Solver.IntParam.RootAlg, 2);

        ArrayList<Pricing> pricings = new ArrayList<Pricing>(data.blocks.size());
        for (Block block : data.blocks)
            pricings.add(Pricing.getNewInstance(block));

        //ArrayList<Pricing> pricingsCPLEX = new ArrayList<Pricing>(data.blocks.size());
        //for (Block block : data.blocks)
        //    pricingsCPLEX.add(new PricingMIP(block));

        int numLambdas[] = new int[data.blocks.size()];
        ArrayList<Double> costs = new ArrayList<Double>();
        boolean hasArtificial = true;
        boolean stop = false;

        PriorityQueue<DoublePair<Integer>> heap = new PriorityQueue<DoublePair<Integer>>(data.blocks.size());
        for (Block block : data.blocks) {
            heap.add(new DoublePair<Integer>(Double.MAX_VALUE, block.index));
        }
        int signal = (model.getObjective().getDirection() == MPObjective.MINIMIZE) ? -1 : 1;

        System.out.println("---------------------------------------------------------------------------------");
        System.out.println("   Iter       Objective         Pricing       Columns      Total         Time   ");
        System.out.println("---------------------------------------------------------------------------------");

        // region Adding optimal solutions of original pricing problems
        //for (int currentBlock = 0; currentBlock < data.blocks.size(); currentBlock++) {
        //    Block block = data.blocks.get(currentBlock);
        //    double originalCosts[] = new double[block.variables.size()];
        //    Pricing pricing = pricings.get(currentBlock);
        //
        //    for (int v = 0; v < block.variables.size(); v++) {
        //        int var = block.variables.get(v);
        //        if (data.model.getObjective().hasVar(var))
        //            originalCosts[v] = data.model.getObjective().getCoeff(var);
        //    }
        //
        //    if (pricing.solve(originalCosts, -Double.MAX_VALUE)) {
        //        for (int s = 0; s < pricing.getSolutions().size(); s++) {
        //            double[] sol = pricing.getSolutions().get(s);
        //
        //            MPVar lambda = new Variable(0.0, Double.MAX_VALUE, true, String.format("lambda(%d,%d)", currentBlock, numLambdas[currentBlock]++));
        //            lambdas.add(model.add(lambda));
        //
        //            double obj = 0.0;
        //            ArrayList<Double> consCoeffs = new ArrayList<Double>(Collections.nCopies(model.getNConstrs(), 0.0));
        //            consCoeffs.set(currentBlock, 1.0);
        //
        //            for (int v = 0; v < sol.length; v++) {
        //                if (Math.abs(sol[v]) < Parameters.EPS)
        //                    continue;
        //
        //                int varIdx = block.variables.get(v);
        //
        //                if (data.varsBlocks.get(varIdx).size() == 1)
        //                    if (data.model.getObjective().hasVar(varIdx))
        //                        obj += data.model.getObjective().getCoeff(varIdx) * sol[v];
        //
        //                if (varsCoeffs.containsKey(varIdx)) {
        //                    for (int c = 0; c < varsCoeffs.get(varIdx).size(); c++) {
        //                        DoublePair<Integer> pair = varsCoeffs.get(varIdx).get(c);
        //                        consCoeffs.set(pair.snd, consCoeffs.get(pair.snd) + pair.fst * sol[v]);
        //                    }
        //                }
        //
        //                if (linkingCoeffs.get(currentBlock).containsKey(varIdx)) {
        //                    for (int c = 0; c < linkingCoeffs.get(varIdx).size(); c++) {
        //                        DoublePair<Integer> pair = linkingCoeffs.get(currentBlock).get(varIdx).get(c);
        //                        consCoeffs.set(pair.snd, consCoeffs.get(pair.snd) + pair.fst * sol[v]);
        //                    }
        //                }
        //            }
        //
        //            costs.add(obj);
        //
        //            if (!hasArtificial)
        //                model.getObjective().addTerm(lambdas.get(lambdas.size() - 1), obj);
        //
        //            for (int c = 0; c < model.getNConstrs(); c++) {
        //                if (consCoeffs.get(c) != 0.0)
        //                    model.constraints.get(c).addTerm(lambdas.get(lambdas.size() - 1), consCoeffs.get(c));
        //                else
        //                    model.constraints.get(c).removeCoeff(lambdas.get(lambdas.size() - 1));
        //            }
        //        }
        //    }
        //}
        //model.updateVariables();
        // endregion Adding optimal solutions of original pricing problems

        int iter = 0;
        while (!stop) {
            model.updateSolver();
            solver.writeModel(params.outFile + "2.lp");
            if (!solver.solve(true)) {
                System.out.println("---------------------------------------------------------------------------------");
                System.out.println("Error: restricted master is infeasible.");
                System.out.println("See file " + params.outFile + ".lp");
                System.out.println("---------------------------------------------------------------------------------");
                solver.writeModel(params.outFile + ".lp");
                System.exit(-1);
            }

            double solution[] = solver.getSolution();

            int numArtificial = 0;
            if (hasArtificial) {
                for (MPVar artificial : artificials) {
                    if ((Math.abs(solution[artificial.getIndex()]) <= Parameters.EPS) && (artificial.getUB() > Parameters.EPS)) {
                        artificial.setUB(0.0);
                        model.updateSolver();
                    }
                    else if (Math.abs(solution[artificial.getIndex()]) > Parameters.EPS) {
                        numArtificial++;
                    }
                }

                if (numArtificial == 0) {
                    hasArtificial = false;
                    createObjective();
                    for (int i = 0; i < lambdas.size(); i++) {
                        if (costs.get(i) != 0)
                            model.getObjective().addTerm(lambdas.get(i), costs.get(i));
                    }
                    model.updateSolver();

                    System.out.println("\n   All artificial variables are zero. Reoptimizing...\n");

                    heap.clear();
                    for (Block block : data.blocks) {
                        heap.add(new DoublePair<Integer>(Double.MAX_VALUE, block.index));
                    }

                    continue;
                }
            }

            int total = 0, blocks = 0;
            double[] duals = solver.getDuals();
            ArrayList<DoublePair<Integer>> done = new ArrayList<DoublePair<Integer>>();
            int currentBlock = 0;

            while (!heap.isEmpty()) {
                currentBlock = heap.remove().snd;

                Block block = data.blocks.get(currentBlock);
                double redCosts[] = new double[block.variables.size()];

                for (int v = 0; v < block.variables.size(); v++) {
                    int varIdx = block.variables.get(v).getIndex();

                    if ((!hasArtificial) && (data.varsBlocks.get(varIdx).size() == 1))
                        if (data.model.getObjective().hasVar(data.model.getVar(varIdx)))
                            redCosts[v] += data.model.getObjective().getCoeff(data.model.getVar(varIdx));

                    if (varsCoeffs.containsKey(varIdx)) {
                        for (int c = 0; c < varsCoeffs.get(varIdx).size(); c++) {
                            DoublePair<Integer> pair = varsCoeffs.get(varIdx).get(c);
                            redCosts[v] -= pair.fst * duals[pair.snd];
                        }
                    }

                    if (linkingCoeffs.get(currentBlock).containsKey(varIdx)) {
                        for (int c = 0; c < linkingCoeffs.get(currentBlock).get(varIdx).size(); c++) {
                            DoublePair<Integer> pair = linkingCoeffs.get(currentBlock).get(varIdx).get(c);
                            redCosts[v] -= pair.fst * duals[pair.snd];
                        }
                    }
                }

                Pricing pricing = pricings.get(currentBlock);
                if (pricing.run(redCosts, -duals[currentBlock])) {
                    for (int s = 0; s < pricing.getSolutions().size(); s++) {
                        double[] sol = pricing.getSolutions().get(s);

                        MPVar lambda = model.addNumVar(0.0, Double.MAX_VALUE, "lambda(%d,%d)", currentBlock, numLambdas[currentBlock]++);
                        //MPVar lambda = model.addVar(0.0, Integer.MAX_VALUE, MPVar.INTEGER, "lambda(%d,%d)", currentBlock, numLambdas[currentBlock]++);
                        lambdas.add(lambda);

                        double obj = 0.0;
                        ArrayList<Double> consCoeffs = new ArrayList<Double>(Collections.nCopies(model.getNConstrs(), 0.0));
                        consCoeffs.set(currentBlock, 1.0);

                        for (int v = 0; v < sol.length; v++) {
                            if (Math.abs(sol[v]) < Parameters.EPS)
                                continue;

                            int varIdx = block.variables.get(v).getIndex();

                            if (data.varsBlocks.get(varIdx).size() == 1)
                                if (data.model.getObjective().hasVar(data.model.getVar(varIdx)))
                                    obj += data.model.getObjective().getCoeff(data.model.getVar(varIdx)) * sol[v];

                            if (varsCoeffs.containsKey(varIdx)) {
                                for (int c = 0; c < varsCoeffs.get(varIdx).size(); c++) {
                                    DoublePair<Integer> pair = varsCoeffs.get(varIdx).get(c);
                                    consCoeffs.set(pair.snd, consCoeffs.get(pair.snd) + pair.fst * sol[v]);
                                }
                            }

                            if (linkingCoeffs.get(currentBlock).containsKey(varIdx)) {
                                for (int c = 0; c < linkingCoeffs.get(varIdx).size(); c++) {
                                    DoublePair<Integer> pair = linkingCoeffs.get(currentBlock).get(varIdx).get(c);
                                    consCoeffs.set(pair.snd, consCoeffs.get(pair.snd) + pair.fst * sol[v]);
                                }
                            }
                        }

                        costs.add(obj);
                        //System.out.printf("obj(%d) = %f\n", costs.size() - 1, obj);

                        if (!hasArtificial)
                            model.getObjective().addTerm(lambdas.get(lambdas.size() - 1), obj);

                        for (int c = 0; c < model.getNConstrs(); c++) {
                            if (consCoeffs.get(c) != 0.0)
                                model.getConstr(c).addTerm(lambdas.get(lambdas.size() - 1), consCoeffs.get(c));
                            else
                                model.getConstr(c).removeVar(lambdas.get(lambdas.size() - 1));
                        }

                        total++;
                    }

                    done.add(new DoublePair<Integer>(signal * pricing.getObjValues().get(0), currentBlock));
                    blocks++;

                    if (params.columnGeneration.runOnce) break;
                }
                else {
                    done.add(new DoublePair<Integer>(0.0, currentBlock));
                }
            }

            for (DoublePair<Integer> pair : done)
                heap.add(pair);
            done.clear();

            model.updateSolver();
            stop = total == 0;

            System.out.printf("%7d   %13.5f   %13.5f   %6d - %-3d   %7d   %9.1fs\n",
              ++iter,
              solver.getObjValue(),
              stop || hasArtificial ? 0.0 : params.columnGeneration.runOnce ? pricings.get(currentBlock).getObjValues().get(0) : -heap.peek().fst,
              total, blocks,
              model.getNVars(),
              (System.currentTimeMillis() - initialTime) / 1000.0);
        }
        System.out.println("---------------------------------------------------------------------------------");
        solver.writeModel(params.outFile + ".lp");

        // exporting the solution in the *chinese-format*
        //new ColumnsExporter(data, model, model).write("out.txt");

        return true;
    }

    private void createVariables() {
        for (int v = 0; v < data.model.getNVars(); v++) {
            if (data.varsBlocks.get(v).isEmpty() || data.varsBlocks.get(v).size() > 1) {
                MPVar originalVar = data.model.getVar(v);
                variables.add(model.addVar(originalVar.getLB(), originalVar.getUB(), originalVar.getType(), originalVar.getName())); // reusing Variable
            }
        }
    }

    private void createConstraints() {
        for (int b = 0; b < data.blocks.size(); b++) {
            String conName = String.format("block(%d)", b);
            MPLinExpr lhs = new MPLinExpr();

            String varName = String.format("a(%d)", artificials.size());
            MPVar artificial = model.addNumVar(0.0, Double.MAX_VALUE, varName);
            artificials.add(artificial);

            model.getObjective().addTerm(artificial, model.getObjective().getDirection() == MPObjective.MINIMIZE ? 1.0 : -1.0);
            lhs.addTerm(artificial, 1.0);

            model.addEq(lhs, 1.0, conName);
        }

        for (int c = 0; c < data.model.getNConstrs(); c++)
            if (data.consBlocks.get(c) < 0) {
                MPLinConstr originalConstr = data.model.getConstr(c);
                MPLinExpr lhs = new MPLinExpr(originalConstr.getConstant());

                for (Map.Entry<MPVar, Double> entry : data.model.getConstr(c).coeffs()) {
                    if (data.varsBlocks.get(entry.getKey().getIndex()).isEmpty()) {
                        lhs.addTerm(model.getVar(entry.getKey().getName()), entry.getValue());
                    }
                    else {
                        if (!varsCoeffs.containsKey(entry.getKey().getIndex()))
                            varsCoeffs.put(entry.getKey().getIndex(), new ArrayList<DoublePair<Integer>>());
                        varsCoeffs.get(entry.getKey().getIndex()).add(new DoublePair<Integer>(entry.getValue(), model.getNConstrs()));
                    }
                }

                MPVar artificial = model.addNumVar(0.0, Double.MAX_VALUE, "a(%d)", artificials.size());
                artificials.add(artificial);

                model.getObjective().addTerm(artificial, model.getObjective().getDirection() == MPObjective.MINIMIZE ? 1.0 : -1.0);
                lhs.addTerm(artificial, -lhs.getConstant() < -Parameters.EPS ? -1.0 : 1.0);

                model.addConstr(lhs, originalConstr.getSense(), 0, originalConstr.getName()); // reusing Constraint
            }

        for (int b = 0; b < data.blocks.size(); b++)
            linkingCoeffs.add(new HashMap<Integer, ArrayList<DoublePair<Integer>>>());

        for (int v = 0; v < data.model.getNVars(); v++) {
            if (data.varsBlocks.get(v).size() > 1) {
                for (int b = 0; b < data.varsBlocks.get(v).size(); b++) {
                    MPLinExpr lhs = new MPLinExpr();
                    lhs.addTerm(model.getVar(data.model.getVar(v).getName()), -1.0);

                    if (!linkingCoeffs.get(data.varsBlocks.get(v).get(b)).containsKey(v))
                        linkingCoeffs.get(data.varsBlocks.get(v).get(b)).put(v, new ArrayList<DoublePair<Integer>>());
                    linkingCoeffs.get(data.varsBlocks.get(v).get(b)).get(v).add(new DoublePair<Integer>(1.0, model.getNConstrs()));

                    model.addEq(lhs, 0, "link(%d)(%s)", data.varsBlocks.get(v).get(b), data.model.getVar(v).getName());
                }
            }
        }
    }

    private void createObjective() {
        for (Map.Entry<MPVar, Double> entry : data.model.getObjective().coeffs())
            if (data.varsBlocks.get(entry.getKey().getIndex()).isEmpty() || data.varsBlocks.get(entry.getKey().getIndex()).size() > 1) {
                MPVar var = entry.getKey();
                model.getObjective().addTerm(model.getVar(var.getName()), entry.getValue());
            }
    }

    //private void loadColumnsFile() {
    //    int numLambdas = 0;
    //    ArrayList<Double> costs = new ArrayList<Double>();
    //
    //    // region Adding column from solution in *chinese-format*
    //    for (int currentBlock = 0; currentBlock < data.blocks.size(); currentBlock++) {
    //        Block block = data.blocks.get(currentBlock);
    //        double originalCosts[] = new double[block.variables.size()];
    //        Pricing pricing = new ColumnsImporter(block, "umps16_lb.txt");// pricings.get(currentBlock);
    //
    //        for (int v = 0; v < block.variables.size(); v++) {
    //            int var = block.variables.get(v);
    //            if (data.model.getObjective().hasVar(var))
    //                originalCosts[v] = data.model.getObjective().getCoeff(var);
    //        }
    //
    //        if (pricing.solve(originalCosts, -Double.MAX_VALUE)) {
    //            for (int s = 0; s < pricing.getSolutions().size(); s++) {
    //                double[] sol = pricing.getSolutions().get(s);
    //
    //                MPVar lambda = new Variable(0.0, Double.MAX_VALUE, true, String.format("col(%d)", numLambdas++));
    //                lambdas.add(model.add(lambda));
    //
    //                double obj = 0.0;
    //                ArrayList<Double> consCoeffs = new ArrayList<Double>(Collections.nCopies(model.getNConstrs(), 0.0));
    //                consCoeffs.set(currentBlock, 1.0);
    //
    //                for (int v = 0; v < sol.length; v++) {
    //                    if (Math.abs(sol[v]) < Parameters.EPS)
    //                        continue;
    //
    //                    int varIdx = block.variables.get(v);
    //
    //                    if (data.varsBlocks.get(varIdx).size() == 1)
    //                        if (data.model.getObjective().hasVar(varIdx))
    //                            obj += data.model.getObjective().getCoeff(varIdx) * sol[v];
    //
    //                    if (varsCoeffs.containsKey(varIdx)) {
    //                        for (int c = 0; c < varsCoeffs.get(varIdx).size(); c++) {
    //                            DoublePair<Integer> pair = varsCoeffs.get(varIdx).get(c);
    //                            consCoeffs.set(pair.snd, consCoeffs.get(pair.snd) + pair.fst * sol[v]);
    //                        }
    //                    }
    //
    //                    if (linkingCoeffs.get(currentBlock).containsKey(varIdx)) {
    //                        for (int c = 0; c < linkingCoeffs.get(varIdx).size(); c++) {
    //                            DoublePair<Integer> pair = linkingCoeffs.get(currentBlock).get(varIdx).get(c);
    //                            consCoeffs.set(pair.snd, consCoeffs.get(pair.snd) + pair.fst * sol[v]);
    //                        }
    //                    }
    //                }
    //
    //                costs.add(obj);
    //
    //                //if (!hasArtificial)
    //                model.getObjective().addTerm(lambdas.get(lambdas.size() - 1), obj);
    //
    //                for (int c = 0; c < model.getNConstrs(); c++) {
    //                    if (consCoeffs.get(c) != 0.0)
    //                        model.constraints.get(c).addTerm(lambdas.get(lambdas.size() - 1), consCoeffs.get(c));
    //                    else
    //                        model.constraints.get(c).removeCoeff(lambdas.get(lambdas.size() - 1));
    //                }
    //            }
    //        }
    //    }
    //
    //    model.updateVariables();
    //    model.writeModel("chinese.lp");
    //    // endregion
    //}

    private class DoublePair<Z> implements Comparable<DoublePair<Z>> {

        public double fst;
        public Z snd;

        public DoublePair(Double fst, Z snd) {
            this.fst = fst;
            this.snd = snd;
        }

        public int compareTo(DoublePair<Z> doublePair) {
            return -Double.compare(fst, doublePair.fst);
        }
    }
}

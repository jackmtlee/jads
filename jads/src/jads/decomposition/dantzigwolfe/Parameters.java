package jads.decomposition.dantzigwolfe;

import jads.mp.*;
import jads.mp.loaders.*;
import jads.mp.solvers.*;

public class Parameters {

    public static final double EPS = 1e-6;
    private static Parameters singleton = new Parameters();

    public boolean continuousInMaster = true;
    public boolean usePresolvedModel = false;
    public int threads = Integer.MAX_VALUE;
    public int randomSeed = 0;

    public String cpartFile;
    public String probFile;
    public String outFile;

    public ColumnGenerationParameters columnGeneration = new ColumnGenerationParameters();
    public PricingParameters pricing = new PricingParameters();
    public Parameters.Solver solver = Solver.gurobi;
    public Parameters.PricingSolver pricingSolver = Parameters.PricingSolver.mip;

    public static Parameters getInstance() {
        return singleton;
    }

    public MPLoader getLoader() {
        switch (solver) {
            case gurobi:
                return new LoaderGurobi();
            case cplex:
                return new LoaderCplex();
            default:
                return null;
        }
    }

    public MPSolver getNewSolver(MPModel model) {
        switch (solver) {
            case gurobi:
                return new SolverGurobi(model, false);
            case cplex:
                return new SolverCplex(model, false);
            default:
                return null;
        }
    }

    public boolean read(String args[], boolean printUsage) {
        if (args.length < 2) {
            if (printUsage) usage();
            return false;
        }

        int index = -1;
        probFile = args[++index];
        outFile = args[++index];

        if (probFile.lastIndexOf(".") > 0) {
            cpartFile = probFile.replace(".lp.gz", "").replace(".lp", "") + ".cpart";
        }
        else {
            if (printUsage) usage();
            return false;
        }

        while (index < args.length - 1) {
            String option = args[++index];

            // Reading general parameters
            if (option.equals("-threads"))
                threads = new Integer(args[++index]);
            else if (option.equals("-presolved"))
                usePresolvedModel = true;

                // Reading pricing parameters
            else if (option.equals("-sollimit"))
                pricing.solLimit = new Integer(args[++index]);
            else if (option.equals("-timelimit"))
                pricing.timeLimit = new Integer(args[++index]);
            else if (option.equals("-timemult"))
                pricing.timeMultiplier = new Integer(args[++index]);
            else if (option.equals("-populate"))
                pricing.populate = true;
            else if (option.equals("-npopulate"))
                pricing.numPopulate = new Integer(args[++index]);

                // Reading column generation parameters

            else if (option.equals("-onepricing"))
                columnGeneration.runOnce = true;
            else if (option.equals("-clique"))
                columnGeneration.genCliques = true;

                // Reading MIP parameters
            else if (option.equals("-tup"))
                pricingSolver = PricingSolver.tup;
            else if (option.equals("-gurobi"))
                solver = Solver.gurobi;
            else if (option.equals("-cplex"))
                solver = Solver.cplex;

            else {
                if (printUsage) usage();
                return false;
            }
        }
        return true;
    }

    private void usage() {
        System.out.println("Usage: ./acg.jar <input_file> <output_file> [options]\n");
        System.out.println("    <input_file>   : path of input model.");
        System.out.println("    <output_file>  : path of the output (solution) file.");
        System.out.println();
        System.out.println("Options:");
        System.out.println();
        System.out.println("    -cplex             : Use Cplex simplex and MIP solver.");
        System.out.println("    -gurobi            : Use Gurobi simplex and MIP solver (default).");
        System.out.println("    -tup               : Use TUP specialized pricing solver.");
        System.out.println();
        System.out.println("    -sollimit <int>    : Solution limit (default = INT_MAX).");
        System.out.println("    -timelimit <int>   : Time limit in secs for each pricing (default = INT_MAX).");
        System.out.println("    -timemult <double> : Time multiplier (default = 2.0).");
        System.out.println("    -populate          : Populate (default = false).");
        System.out.println("    -numpopulate <int> : Populate solutions limit (default = 20).");
        System.out.println();
        System.out.println("    -onepricing        : Resolve the master after every pricing with new columns.");
        //System.out.println("    -clique            : Generate clique cuts.");
        System.out.println();
        System.out.println("    -presolved         : Use presolved model in the column generation.");
        System.out.println();
    }

    public enum PricingSolver {
        mip, tup,
    }

    public enum Solver {
        cplex, gurobi,
    }

    public class ColumnGenerationParameters {

        public boolean runOnce = false;
        public boolean genCliques = false;
    }

    public class PricingParameters {

        public int solLimit = Integer.MAX_VALUE;
        public boolean populate = false;
        public int numPopulate = 20;
        public double timeLimit = Double.MAX_VALUE;
        public double timeMultiplier = 2;
    }
}

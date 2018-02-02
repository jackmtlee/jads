package jads.decomposition.heuristic;

import com.google.gson.*;
import jads.mp.*;
import jads.mp.loaders.*;
import jads.mp.solvers.*;
import jads.mp.util.*;

import java.io.*;
import java.nio.file.*;
import java.util.*;
import java.util.zip.*;

/**
 * This class contains a singleton object with the data required by the
 * algorithm.
 *
 * @author Tulio Toffolo
 */
public class Data {

    public static final double EPS = 1e-6;
    private static Data singleton = new Data();

    public String name = "";
    public MPModel model = new MPModel("");
    public Solution initialSolution = null;

    public Random random = new Random();
    public List<Decomposition> decompositions = new ArrayList<>();

    public int randomSeed = 2;
    public long timeLimitMillis = Long.MAX_VALUE;
    public int threads = Integer.MAX_VALUE;

    public String jdecFile = null, probFile = null, outFile = null, iniSolFile = null;

    public Data.Loader loader = Loader.jads;
    public Data.Solver solver = Solver.gurobi;
    public Data.SubproblemParameters subproblem = new SubproblemParameters();


    private Data() {}


    public MPLoader getLoader() {
        switch (loader) {
            case cplex:
                return new LoaderCplex();
            case gurobi:
                return new LoaderGurobi();
            case jads:
                return new LoaderMP();
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

    public boolean loadData() {
        return loadJdec();
    }

    public boolean read(String args[], boolean printUsage) {
        if (args.length < 2) {
            if (printUsage) printUsage();
            return false;
        }

        int index = -1;
        jdecFile = args[++index];
        outFile = args[++index];

        while (index < args.length - 1) {
            String option = args[++index];

            switch (option) {
                // Reading general parameters
                case "-sol":
                    iniSolFile = args[++index];
                    break;
                case "-seed":
                    randomSeed = new Integer(args[++index]);
                    random = new Random(randomSeed);
                    break;
                case "-threads":
                    threads = new Integer(args[++index]);
                    break;
                case "-timelimit":
                    timeLimitMillis = ( long ) (new Double(args[++index]) * 60_000);
                    break;

                // reading solver parameters
                case "-gurobi":
                    solver = Solver.gurobi;
                    break;
                case "-cplex":
                    solver = Solver.cplex;
                    break;

                // reading subproblem parameters
                case "-sollimit":
                    subproblem.solLimit = new Integer(args[++index]);
                    break;
                case "-subtimelimit":
                    subproblem.timeLimitMillis = ( long ) (new Double(args[++index]) * 60_000);
                    break;

                default:
                    if (printUsage) printUsage();
                    return false;
            }
        }
        return true;
    }


    private boolean loadJdec() {
        try {
            BufferedReader bufferedReader;
            if (jdecFile.endsWith(".gz"))
                bufferedReader = new BufferedReader(new InputStreamReader(new GZIPInputStream(new FileInputStream(jdecFile))));
            else
                bufferedReader = new BufferedReader(new InputStreamReader(new FileInputStream(jdecFile)));
            System.out.println("Loading decomposition(s) from " + jdecFile + "... ");

            JsonParser parser = new JsonParser();
            JsonObject json = ( JsonObject ) parser.parse(bufferedReader);

            // loading problem description (model)
            probFile = Paths.get(Paths.get(jdecFile).getParent().toString(), json.get("problem").getAsString()).toString();
            if (json.has("initial_solution"))
                iniSolFile = Paths.get(Paths.get(jdecFile).getParent().toString(), json.get("initial_solution").getAsString()).toString();

            if (!loadModel()) return false;
            System.out.println();

            JsonArray jsonDecompositions = json.getAsJsonArray("decompositions");

            for (JsonElement jsonDecItem : jsonDecompositions) {
                JsonObject jsonDec = jsonDecItem.getAsJsonObject();
                Decomposition decomposition = new Decomposition(model, jsonDec, random);
                decompositions.add(decomposition);

                System.out.printf("Found decomposition%s with %d blocks and %d variables in these blocks.\n", decomposition.name.isEmpty() ? "" : " '" + decomposition.name + "'", decomposition.blocks.size(), decomposition.varsCount);
                if (model.getNVars() > decomposition.varsCount)
                    System.out.printf("There are %d linking variables.\n", model.getNVars() - decomposition.varsCount);
                System.out.println();
            }

            decompositions.sort(Comparator.comparingInt(a -> a.priority));
            for (int i = 0; i < decompositions.size(); i++)
                decompositions.get(i).setIndex(i);

            bufferedReader.close();
        }

        catch (IOException e) {
            System.out.println("\n" + e.getMessage() + "\n");
            return false;
        }

        return true;
    }

    private boolean loadModel() {
        MPLoader loader = getLoader();
        model = loader.loadModel(probFile);
        if (model == null)
            return false;

        name = Paths.get(probFile).getFileName().toString().replace(".gz", "").replace(".lp", "").replace(".mps", "");

        // reading initial solution (if any is specified)
        if (iniSolFile != null) loadSolution();

        return true;
    }

    private void loadSolution() {
        initialSolution = new Solution(model);

        try {
            BufferedReader bufferedReader;
            if (jdecFile.endsWith(".gz"))
                bufferedReader = new BufferedReader(new InputStreamReader(new GZIPInputStream(new FileInputStream(iniSolFile))));
            else
                bufferedReader = new BufferedReader(new InputStreamReader(new FileInputStream(iniSolFile)));
            System.out.println("Loading initial solution from " + iniSolFile + "... ");

            String line;
            while ((line = bufferedReader.readLine()) != null) {
                if (line.startsWith("#"))
                    continue;

                SimpleTokenizer tokens = new SimpleTokenizer(line, " ");
                String varName = tokens.nextToken();
                double value = tokens.nextDouble();

                initialSolution.setValue(varName, value);
            }
        }
        catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void printUsage() {
        System.out.println("Usage: ./jads.jar <input_file> <output_file> [options]\n");
        System.out.println("    <input_file>  : path of input jdec file.");
        System.out.println("    <output_file> : path of the output (solution) file.");
        System.out.println();
        System.out.println("Options:");
        System.out.println();
        System.out.println("    -sol <sol_file>    : Initial solution (default = NULL).");
        System.out.println();
        System.out.println("    -seed <int>        : Random seed (default = " + randomSeed + ").");
        System.out.println("    -timelimit <double>: Time limit in minutes (default = INF).");
        System.out.println();
        System.out.println("    -cbc               : Use CBC solver" + (solver == Solver.cbc ? " (default)" : "") + ".");
        System.out.println("    -cplex             : Use Cplex solver" + (solver == Solver.cplex ? " (default)" : "") + ".");
        System.out.println("    -gurobi            : Use Gurobi solver" + (solver == Solver.gurobi ? " (default)" : "") + ".");
        System.out.println("    -scip              : Use SCIP solver" + (solver == Solver.scip ? " (default)" : "") + ".");
        System.out.println();
        System.out.println("    -sollimit <int>    : Solution limit (default = INF).");
        System.out.println("    -subtlimit <double>: Time limit in minutes for each subproblem (default = INF).");
        System.out.println();
    }


    public static Data getInstance() {
        return singleton;
    }


    public enum Loader {
        cplex, gurobi, jads
    }

    public enum Solver {
        cbc, cplex, gurobi, scip,
    }

    public class SubproblemParameters {

        public int solLimit = 3;
        public long timeLimitMillis = Long.MAX_VALUE;
    }
}

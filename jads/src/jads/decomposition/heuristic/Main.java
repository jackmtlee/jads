package jads.decomposition.heuristic;

import jads.decomposition.heuristic.constructive.*;
import jads.decomposition.heuristic.localsearch.*;

import java.io.*;
import java.util.*;

/**
 * This class contains an initial solver implementation. It will be replaced by
 * an easier-to-use solver in the near future.
 *
 * @author Tulio Toffolo
 */
public class Main {

    public static void main(String[] args) throws IOException {
        //System.in.read();

        Locale.setDefault(new Locale("en-US"));
        long startTimeMillis = System.currentTimeMillis();

        // reading arguments
        Data data = Data.getInstance();
        if (!data.read(args, true)) return;

        // reading formulation, decomposition and subproblems
        if (!data.loadData()) return;
        Solution solution = data.initialSolution;

        double loadingRuntime = (System.currentTimeMillis() - startTimeMillis) / 1000.0;
        System.out.printf("Instance file: %s\n", Data.getInstance().name);
        System.out.printf("Loading runtime: %.2f seconds\n", loadingRuntime);
        System.out.println();
        startTimeMillis = System.currentTimeMillis();

        // calling constructive if there is no initial solution
        Constructive constructive = new Constructive(3, 3);
        if (solution == null)
            solution = constructive.solve(data.model);
        else
            solution = constructive.completeSolution(data.model, solution);

        // printing constructive algorithm statistics
        double constructiveRuntime = (System.currentTimeMillis() - startTimeMillis) / 1000.0;
        System.out.println();
        System.out.printf("Instance file: %s\n", Data.getInstance().name);
        if (data.initialSolution != null)
            System.out.printf("Initial solution file: %s\n", Data.getInstance().iniSolFile);
        System.out.printf("Objective value: %s\n", solution != null ? solution.getObjective() : "infeasible");
        System.out.printf("Constructive runtime: %.2f seconds\n", constructiveRuntime);
        System.out.printf("Total wall-clock runtime: %.2f seconds\n", loadingRuntime + constructiveRuntime);
        System.out.println();
        startTimeMillis = System.currentTimeMillis();

        if (solution != null) {
            LocalSearch localSearch = new LocalSearch(data.model, 4, 2, true);
            solution = localSearch.solve(solution, System.currentTimeMillis() + 900 * 1000);
            System.out.println();
        }

        double localSearchRuntime = (System.currentTimeMillis() - startTimeMillis) / 1000.0;
        System.out.printf("Instance file: %s\n", Data.getInstance().name);
        System.out.printf("Objective value: %s\n", solution != null ? solution.getObjective() : "infeasible");
        System.out.printf("Constructive runtime: %.2f seconds\n", constructiveRuntime);
        System.out.printf("Local search runtime: %.2f seconds\n", localSearchRuntime);
        System.out.printf("Total wall-clock runtime: %.2f seconds\n", loadingRuntime + constructiveRuntime + localSearchRuntime);
    }
}

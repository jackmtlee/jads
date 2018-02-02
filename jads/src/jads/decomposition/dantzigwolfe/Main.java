package jads.decomposition.dantzigwolfe;

import jads.mp.*;

import java.util.*;

public class Main {

    public static void main(String[] args) {
        Locale.setDefault(new Locale("en-US"));
        long startTimeMillis = System.currentTimeMillis();

        Data data = Data.getInstance();
        Parameters params = Parameters.getInstance();
        if (!params.read(args, true)) return;

        // reading formulation
        MPLoader loader = params.getLoader();
        data.model = loader.loadModel(params.probFile);
        if (data.model == null) return;
        data.name =  data.model.getName();//.substring(0, data.model.getName().indexOf('.'));

        // reading block
        BlockLoader blockLoader = new BlockLoader(params, data);
        if (!blockLoader.loadBlocks()) return;

        ColumnGeneration columnGeneration = new ColumnGeneration();
        columnGeneration.solve();

        System.out.printf("Total wall-clock runtime: %.2f seconds\n", (System.currentTimeMillis() - startTimeMillis) / 1000.0);
        System.out.println();
        System.out.printf("Instance files: %s\n", Data.getInstance().name);
        System.out.printf("LP objective value: %f\n", columnGeneration.solver.getObjValue());
    }
}

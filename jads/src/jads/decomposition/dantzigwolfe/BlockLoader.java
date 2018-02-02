package jads.decomposition.dantzigwolfe;

import jads.mp.*;

import java.io.*;
import java.util.*;

public class BlockLoader {

    private final Parameters params;
    private final Data data;

    public BlockLoader(Parameters params, Data data) {
        this.params = params;
        this.data = data;
    }

    public boolean loadBlocks() {
        if (loadBlocksCpart() || loadBlocksVpart()) {
            writeBlocksDec();
            return true;
        }
        return loadBlocksDec();
    }

    private boolean loadBlocksCpart() {
        String filename = params.cpartFile;
        if (filename.isEmpty())
            filename = params.probFile.substring(0, params.probFile.indexOf('.')) + ".cpart";

        try {
            BufferedReader bufferedReader = new BufferedReader(new FileReader(new File(filename)));
            System.out.println("Loading blocks from " + data.name + ".cpart... ");

            data.consBlocks = new ArrayList<Integer>(Collections.nCopies(data.model.getNConstrs(), -1));

            for (int v = 0; v < data.model.getNVars(); v++)
                data.varsBlocks.add(new ArrayList<Integer>());

            int nBlocks = new Integer(bufferedReader.readLine());
            for (int b = 0; b < nBlocks; b++) {

                data.blocks.add(new Block(b));

                int nConstraints = new Integer(bufferedReader.readLine());

                for (int c = 0; c < nConstraints; c++) {
                    String conName = bufferedReader.readLine();

                    boolean continuous = false;
                    int consIndex = data.model.getConstr(conName).getIndex();
                    final MPLinConstr cons = data.model.getConstr(consIndex);

                    for (Map.Entry<MPVar, Double> entry : cons.coeffs()) {
                        if (params.continuousInMaster && !entry.getKey().isInteger()) {
                            continuous = true;
                            continue;
                        }

                        if (data.varsBlocks.get(entry.getKey().getIndex()).isEmpty() || data.varsBlocks.get(entry.getKey().getIndex()).get(data.varsBlocks.get(entry.getKey().getIndex()).size() - 1) != b)
                            data.varsBlocks.get(entry.getKey().getIndex()).add(b);
                    }

                    if (!params.continuousInMaster || !continuous) {
                        data.blocks.get(b).constraints.add(cons);
                        data.blocks.get(b).consIndexes.put(consIndex, c);
                        data.consBlocks.set(consIndex, b);
                    }
                }
            }

            int total = 0, linking = 0;

            for (int v = 0; v < data.varsBlocks.size(); v++) {
                if (data.varsBlocks.get(v).size() > 1)
                    linking++;

                MPVar var = data.model.getVar(v);
                for (int b = 0; b < data.varsBlocks.get(v).size(); b++) {
                    data.blocks.get(data.varsBlocks.get(v).get(b)).variables.add(var);
                    data.blocks.get(data.varsBlocks.get(v).get(b)).varsIndexes.put(v, data.blocks.get(data.varsBlocks.get(v).get(b)).variables.size() - 1);
                    total++;
                }
            }

            bufferedReader.close();

            System.out.println("Found " + data.blocks.size() + " blocks and " + total + " variables in these blocks.");
            if (linking > 0)
                System.out.println("There are " + linking + " linking variables.");
            System.out.println();
        }
        catch (IOException e) {
            //System.out.println("\n" + e.getMessage() + "\n");
            return false;
        }

        return true;
    }

    private boolean loadBlocksDec() {
        String filename = params.cpartFile;
        if (filename.isEmpty())
            filename = params.probFile.substring(0, params.probFile.indexOf('.')) + ".cpart";

        filename = filename.replace("cpart", "dec");

        String temp = "";

        try {
            BufferedReader bufferedReader = new BufferedReader(new FileReader(new File(filename)));
            System.out.println("Loading blocks from " + data.name + ".dec... ");

            data.consBlocks = new ArrayList<Integer>(Collections.nCopies(data.model.getNConstrs(), -1));

            for (int v = 0; v < data.model.getNVars(); v++)
                data.varsBlocks.add(new ArrayList<Integer>());

            while (temp != null && (temp.length() < 5 || !temp.substring(0, 5).equals("BLOCK")))
                temp = bufferedReader.readLine();

            int b = 0;
            while (temp != null) {
                if (temp.length() > 5 && temp.substring(0, 5).equals("BLOCK")) {
                    data.blocks.add(new Block(b));

                    int c = 0;
                    temp = bufferedReader.readLine();
                    while (temp != null && !(temp.length() > 5 && temp.substring(0, 5).equals("BLOCK")) && !(temp.length() >= 11 && temp.substring(0, 11).equals("MASTERCONSS"))) {
                        String conName = temp;
                        boolean continuous = false;
                        int consIndex = data.model.getConstr(conName).getIndex();
                        final MPLinConstr cons = data.model.getConstr(consIndex);

                        for (Map.Entry<MPVar, Double> entry : cons.coeffs()) {
                            if (params.continuousInMaster && !entry.getKey().isInteger()) {
                                continuous = true;
                                continue;
                            }

                            if (data.varsBlocks.get(entry.getKey().getIndex()).isEmpty() || data.varsBlocks.get(entry.getKey().getIndex()).get(data.varsBlocks.get(entry.getKey().getIndex()).size() - 1) != b)
                                data.varsBlocks.get(entry.getKey().getIndex()).add(b);
                        }

                        if (!params.continuousInMaster || !continuous) {
                            data.blocks.get(b).constraints.add(cons);
                            data.blocks.get(b).consIndexes.put(consIndex, c++);
                            data.consBlocks.set(consIndex, b);
                        }
                        temp = bufferedReader.readLine();
                    }
                    b++;
                }
                else {
                    temp = bufferedReader.readLine();
                }
            }
            int total = 0, linking = 0;

            for (int v = 0; v < data.varsBlocks.size(); v++) {
                if (data.varsBlocks.get(v).size() > 1)
                    linking++;

                MPVar var = data.model.getVar(v); ;
                for (int block = 0; block < data.varsBlocks.get(v).size(); block++) {
                    data.blocks.get(data.varsBlocks.get(v).get(block)).variables.add(var);
                    data.blocks.get(data.varsBlocks.get(v).get(block)).varsIndexes.put(v, data.blocks.get(data.varsBlocks.get(v).get(block)).variables.size() - 1);
                    total++;
                }
            }

            bufferedReader.close();

            System.out.println("Found " + data.blocks.size() + " blocks and " + total + " variables in these blocks.");
            if (linking > 0)
                System.out.println("There are " + linking + " linking variables.");
            System.out.println();
        }
        catch (IOException e) {
            System.out.println("\n" + e.getMessage() + "\n");
            return false;
        }

        return true;
    }

    private boolean loadBlocksVpart() {
        String filename = params.cpartFile.replace("cpart", "vpart");
        if (filename.isEmpty())
            filename = params.probFile.substring(0, params.probFile.indexOf('.')) + ".vpart";

        try {
            System.out.println("Loading blocks from " + data.name + ".vpart... ");

            BufferedReader bufferedReader = new BufferedReader(new FileReader(new File(filename)));

            data.consBlocks = new ArrayList<Integer>(Collections.nCopies(data.model.getNConstrs(), -1));

            for (int v = 0; v < data.model.getNVars(); v++)
                data.varsBlocks.add(new ArrayList<Integer>());

            // obtaining block of each variable
            int nBlocks = new Integer(bufferedReader.readLine());
            for (int b = 0; b < nBlocks; b++) {
                data.blocks.add(new Block(b));

                int nVariables = new Integer(bufferedReader.readLine());

                for (int v = 0; v < nVariables; v++) {
                    String varName = bufferedReader.readLine();
                    int varIndex = data.model.getVar(varName).getIndex();
                    data.varsBlocks.get(varIndex).add(b);
                }
            }

            for (int consIndex = 0; consIndex < data.model.getNConstrs(); consIndex++) {
                boolean continuous = false;
                final MPLinConstr cons = data.model.getConstr(consIndex);

                int block = -1;
                for (Map.Entry<MPVar, Double> entry : cons.coeffs()) {
                    if (params.continuousInMaster && !entry.getKey().isInteger()) {
                        continuous = true;
                        continue;
                    }

                    if (data.varsBlocks.get(entry.getKey().getIndex()).size() == 0)
                        continue;

                    if (block >= 0 && data.varsBlocks.get(entry.getKey().getIndex()).get(0) != block) {
                        block = -1;
                        break;
                    }
                    else {
                        block = data.varsBlocks.get(entry.getKey().getIndex()).get(0);
                    }
                }
                if (block >= 0) data.consBlocks.set(consIndex, block);
            }

            int total = 0, linking = 0;

            for (int v = 0; v < data.varsBlocks.size(); v++) {
                MPVar var = data.model.getVar(v); ;
                for (int b = 0; b < data.varsBlocks.get(v).size(); b++) {
                    data.blocks.get(data.varsBlocks.get(v).get(b)).variables.add(var);
                    data.blocks.get(data.varsBlocks.get(v).get(b)).varsIndexes.put(v, data.blocks.get(data.varsBlocks.get(v).get(b)).variables.size() - 1);
                    total++;
                }

                if (data.varsBlocks.get(v).size() == 0) {
                    data.varsBlocks.get(v).add(-1);
                    linking++;
                }
            }

            bufferedReader.close();

            System.out.println("Found " + data.blocks.size() + " blocks and " + total + " variables in these blocks.");
            if (linking > 0)
                System.out.println("There are " + linking + " linking variables.");
            System.out.println();
        }
        catch (IOException e) {
            System.out.println("\n" + e.getMessage() + "\n");
            return false;
        }

        return true;
    }

    private boolean writeBlocksDec() {
        String filename = params.cpartFile;
        if (filename.isEmpty())
            filename = params.probFile.substring(0, params.probFile.indexOf('.')) + ".cpart";

        filename = filename.replace("cpart", "dec");

        try {
            PrintWriter writer = new PrintWriter(new BufferedWriter(new FileWriter(new File(filename))));
            System.out.println("Writing blocks to " + data.name + ".dec... ");

            // writing header
            writer.printf("PRESOLVED\n%d\nNBLOCKS\n%d\n", 0, data.blocks.size());

            // writing blocks
            for (int block = 0; block < data.blocks.size(); block++) {
                writer.printf("BLOCK %d\n", block + 1);
                for (int constr = 0; constr < data.consBlocks.size(); constr++) {
                    if (data.consBlocks.get(constr) == block)
                        writer.printf("%s\n", data.model.getConstr(constr).getName());
                }
            }

            // writing master constraints
            writer.printf("MASTERCONSS\n");
            for (int constr = 0; constr < data.consBlocks.size(); constr++) {
                if (data.consBlocks.get(constr) == -1)
                    writer.printf("%s\n", data.model.getConstr(constr).getName());
            }

            writer.close();
        }
        catch (IOException e) {
            System.out.println("\n" + e.getMessage() + "\n");
            return false;
        }

        return true;
    }
}

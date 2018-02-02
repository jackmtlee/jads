package jads.decomposition.heuristic;

import com.google.gson.*;
import jads.mp.*;

import java.io.*;
import java.nio.file.*;
import java.util.*;

/**
 * This class represents a Decomposition.
 *
 * @author Tulio Toffolo
 */
public class Decomposition {

    public String name = "";
    public String submodelFile = null;
    public int priority = 0;
    public int eta = 0;
    public int step = 0;
    public int etaSkip = 1;
    public int maxStep = Integer.MAX_VALUE;
    public int maxEta = Integer.MAX_VALUE;
    public boolean shuffle = false;

    public int varsCount;

    public List<Block> blocks = new ArrayList<>();
    public List<List<Block>> varsBlocks = new ArrayList<>();
    public List<List<Block>> consBlocks = new ArrayList<>();
    public Map<String, Block> blockMap = new HashMap<>();

    public MPModel submodel;

    private Random random;
    private int index = -1;

    public Decomposition(MPModel originalModel, JsonObject json, Random random) {
        this.name = name;
        this.submodel = new MPModel(name);
        this.random = random;

        // initializing list of lists
        for (int i = 0; i < originalModel.getNVars(); i++)
            varsBlocks.add(new ArrayList<>());
        for (int i = 0; i < originalModel.getNConstrs(); i++)
            consBlocks.add(new ArrayList<>());

        // loading subproblem characteristics
        loadJson(originalModel, json);
        loadSubproblem();
    }

    public int getIndex() {
        return index;
    }

    public void setIndex(int index) {
        this.index = index;
    }

    private void loadJson(MPModel originalModel, JsonObject json) {
        varsCount = 0;

        // reading properties
        if (json.has("name"))
            name = json.get("name").getAsString();
        if (json.has("model"))
            submodelFile = Paths.get(Data.getInstance().probFile).getParent().toString() + "/" + json.get("model").getAsString();
        if (json.has("priority"))
            priority = json.get("priority").getAsInt();
        if (json.has("eta"))
            eta = json.get("eta").getAsInt();
        if (json.has("step"))
            step = json.get("step").getAsInt();
        if (json.has("eta_skip"))
            etaSkip = json.get("eta_skip").getAsInt();
        if (json.has("max_eta"))
            maxEta = json.get("max_eta").getAsInt();
        if (json.has("max_step"))
            maxStep = json.get("max_step").getAsInt();
        if (json.has("shuffle"))
            shuffle = json.get("shuffle").getAsBoolean();

        // reading subproblems
        JsonArray jsonSubproblems = json.getAsJsonArray("subproblems");
        for (JsonElement elem : jsonSubproblems) {
            JsonObject jsonObj = elem.getAsJsonObject();
            Block block = new Block(jsonObj.get("priority").getAsInt(), jsonObj.get("name").getAsString());

            JsonArray jsonVars = jsonObj.getAsJsonArray("variables");
            for (JsonElement var : jsonVars) {
                MPVar variable = originalModel.getVar(var.getAsString());
                block.addVar(variable);

                if (varsBlocks.get(variable.getIndex()).size() == 0)
                    varsCount++;
                varsBlocks.get(variable.getIndex()).add(block);
            }

            blocks.add(block);
            blockMap.put(block.name, block);
        }

        // sorting subproblems and setting indexes
        Collections.shuffle(blocks, random);
        blocks.sort(Comparator.comparingInt(a -> a.priority));
        for (int i = 0; i < blocks.size(); i++) {
            blocks.get(i).setIndex(i);
        }

        // reading connections
        JsonArray jsonConnections = json.getAsJsonArray("connections");
        for (JsonElement elem : jsonConnections) {
            JsonObject jsonObj = elem.getAsJsonObject();

            int priority = jsonObj.get("priority").getAsInt();
            Block src = blockMap.get(jsonObj.get("src").getAsString());
            Block tar = blockMap.get(jsonObj.get("tar").getAsString());

            assert src != null;
            assert tar != null;
            src.addConnection(priority, tar);
        }

        // sorting connections
        blocks.forEach(block -> block.sortConnections(random));
    }

    private void loadSubproblem() {
        if (submodelFile == null) return;

        if (new File(submodelFile).exists()) {
            MPLoader loader = Data.getInstance().getLoader();
            submodel = loader.loadModel(submodelFile);
        }
        else {
            submodel = new MPModel();
        }

        //try {
        //String outputPath = String.format("subproblem-%s.lp", uuid);
        //Path path = Paths.get(submodelFile);
        //String content = new String(Files.readAllBytes(path));

        // replacing special keywords for their corresponding values
        //content = content.replaceAll("\\{\\{[ ]*index[ ]*}}", Integer.toString(index));
        //content = content.replaceAll("\\{\\{[ ]*eta[ ]*}}", Integer.toString(eta));

        // writing temporary file with formulation
        //Files.write(Paths.get(outputPath), content.getBytes());
        //String outputPath = submodelFile;


        // removing temporary file with formulation
        //Files.delete(Paths.get(outputPath));
        //}
        //catch (IOException e) {
        //    e.printStackTrace();
        //}
    }
}

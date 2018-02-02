package jads.decomposition.dantzigwolfe;

import jads.mp.*;

import java.util.*;

public class Data {

    public MPModel model = new MPModel("original");
    public List<Block> blocks = new ArrayList<Block>();
    public List<List<Integer>> varsBlocks = new ArrayList<List<Integer>>();
    public List<Integer> consBlocks = new ArrayList<Integer>();
    public String name = "";

    private static Data singleton = null;

    private Data() { }

    public static Data getInstance() {
        if (singleton == null)
            singleton = new Data();
        return singleton;
    }
}

package jads.decomposition.dantzigwolfe;

import jads.mp.*;

import java.util.*;

public class Block {

    public final int index;
    public final List<MPLinConstr> constraints = new ArrayList<MPLinConstr>();
    public final List<MPVar> variables = new ArrayList<MPVar>();

    public final HashMap<Integer, Integer> varsIndexes = new HashMap<Integer, Integer>();
    public final HashMap<Integer, Integer> consIndexes = new HashMap<Integer, Integer>();

    public Block(int index) {
        this.index = index;
    }
}
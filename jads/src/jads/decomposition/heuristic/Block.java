package jads.decomposition.heuristic;

import jads.mp.*;
import jads.mp.util.*;

import java.util.*;

/**
 * This class represents a Block (or minimal subproblem).
 *
 * @author Tulio Toffolo
 */
public class Block {

    public final int priority;
    public final String name;

    private int index;

    private final List<PairInt<Block>> connections = new ArrayList<>();
    private final List<MPLinConstr> constraints = new ArrayList<>();
    private final Set<MPLinConstr> constraintSet = new HashSet<>();
    private final List<MPVar> variables = new ArrayList<>();
    private final List<MPVar> variableSet = new ArrayList<>();

    public Block(int index, int priority, String name) {
        this.index = index;
        this.priority = priority;
        this.name = name;
    }

    public Block(int priority, String name) {
        this(-1, priority, name);
    }

    public void addConnection(int priority, Block block) {
        connections.add(new PairInt<>(priority, block));
    }

    public void addVar(MPVar var) {
        if (!variableSet.contains(var)) {
            variables.add(var);
            variableSet.add(var);

            for (Map.Entry<MPLinConstr, Double> entry : var.coeffs()) {
                if (!constraintSet.contains(entry.getKey())) {
                    constraints.add(entry.getKey());
                    constraintSet.add(entry.getKey());
                }
            }
        }
    }

    public Iterable<MPLinConstr> constrs() {
        return constraints;
    }

    public Iterable<PairInt<Block>> getConnections() {
        return connections;
    }

    public int getIndex() {
        return index;
    }

    public Iterable<MPVar> vars() {
        return variables;
    }

    public void setIndex(int index) {
        this.index = index;
    }

    public void sortConnections(Random random) {
        if (connections.size() > 1) {
            Collections.shuffle(connections, random);
            connections.sort(Comparator.comparingInt(a -> a.first));
        }
    }
}

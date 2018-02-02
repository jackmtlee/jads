package jads.decomposition.dantzigwolfe;

import java.util.*;

public abstract class Pricing extends Algorithm {

    protected Block block;
    protected ArrayList<Double> objValues = new ArrayList<Double>();
    protected ArrayList<double[]> solutions = new ArrayList<double[]>();

    public static Pricing getNewInstance(Block block) {
        Parameters params = Parameters.getInstance();

        if (params.pricingSolver == Parameters.PricingSolver.mip)
            return new PricingMIP(block);
        //else if (params.pricingSolver == Parameters.PricingSolver.tup)
        //    return new PricingTUP(block);

        return null;
    }

    public Pricing(Block _block) {
        block = _block;
    }

    public final List<Double> getObjValues() {
        return objValues;
    }

    public final ArrayList<double[]> getSolutions() {
        return solutions;
    }

    public abstract boolean run(double redCosts[], double fixedCost);
}

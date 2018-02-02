package jads.mp.test;

import jads.mp.*;
import jads.mp.solvers.*;
import jads.mp.*;
import jads.mp.solvers.*;

public class MPTest {

    public static void main(String args[]) {
        MPModel model = new MPModel("teste");

        MPVar x1 = model.addVar(0.0, 1.0, 'B', "x1");
        MPVar x2 = model.addVar(0.0, 1.0, 'B', "x2");

        MPLinConstr c1 = model.addConstr(new MPLinExpr(new MPVar[]{ x1, x2 }, new double[]{ 1.1, 2.0 }), '<', 1.0, "c1");

        c1.addTerm(0.5, x1);

        System.out.println(x1.getCoeff(c1));

        model.setSolver(new SolverGurobi(model, false));
        model.getSolver().solve();
        model.getSolver().writeModel("teste.lp");
    }
}

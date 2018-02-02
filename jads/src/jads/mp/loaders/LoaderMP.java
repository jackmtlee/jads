package jads.mp.loaders;

import jads.mp.*;
import jads.mp.util.*;

import java.io.*;
import java.util.zip.*;

/**
 * This class provides a means of loading an LP (CPLEX-formatted) or MPS file.
 */
public class LoaderMP implements MPLoader {

    public MPModel loadModel(String filePath) {
        BufferedReader bufferedReader;
        System.out.println("Loading file " + filePath + "...");

        try {
            if (filePath.endsWith(".gz"))
                bufferedReader = new BufferedReader(new InputStreamReader(new GZIPInputStream(new FileInputStream(filePath))));
            else
                bufferedReader = new BufferedReader(new InputStreamReader(new FileInputStream(filePath)));

            MPModel model = null;

            // if model is specified by a MPS file
            if (filePath.endsWith(".mps") || filePath.endsWith(".mps.gz")) {
                model = loadMPS(bufferedReader);
            }

            // if model is specified by an LP file
            else {
                // lpsolve LP-format is preferred
                // Cplex and Gurobi's format are compatible but must be specified
                model = loadLP(bufferedReader);
            }

            bufferedReader.close();
            return model;
        }
        catch (IOException e) {
            e.printStackTrace();
            return null;
        }
    }

    // TODO: improve LP reader (it is currently not very robust)
    public MPModel loadLP(BufferedReader reader) throws IOException {
        // creating the new model
        MPModel model = new MPModel();

        // auxiliary variables
        Constr constr = new Constr();
        Expr expr = new Expr();
        MPVar var;
        String next;
        Section section = Section.Objective;

        // reading first line
        String line = reader.readLine();
        LPTokenizer tokenizer = new LPTokenizer(line);

        while (tokenizer.hasToken() || (line = reader.readLine()) != null) {
            if (!tokenizer.hasToken()) tokenizer = new LPTokenizer(line);
            if (!tokenizer.hasToken()) continue;

            next = tokenizer.nextToken();

            // if line is a comment
            if (next != null && next.charAt(0) == '\\') {
                tokenizer.finish(); // skip the entire line since it corresponds to a comment
            }

            // reading objective function
            else if (section == Section.Objective) {
                if (isLPKeyword(next)) {
                    section = null;
                    tokenizer.undo();
                }
                else if (tokenizer.isNumber()) {
                    if (expr == null) expr = new Expr();
                    if (next.length() == 1 && next.charAt(0) == '+')
                        expr.coeff = 1.0;
                    else if (next.length() == 1 && next.charAt(0) == '-')
                        expr.coeff = -1.0;
                    else
                        expr.coeff = expr.coeff != null ? expr.coeff * new Double(next) : new Double(next);
                }
                else if (expr == null && next.contains(":")) {
                    section = Section.SubjectTo;
                    tokenizer.undo();
                }
                else if (expr != null) {
                    expr.varname = next;
                    model.addNumVar(0, Double.MAX_VALUE, expr.coeff != null ? expr.coeff : 1, expr.varname);
                    expr = null;
                }
                else {
                    throw new Error("Unexpected word in LP file.");
                }
            }

            // reading constraints
            else if (section == Section.SubjectTo) {
                if (isLPKeyword(next)) {
                    section = null;
                    tokenizer.undo();
                }
                else if (next.endsWith(":")) {
                    if (constr.sense != '\0') {
                        if (expr != null)
                            constr.rhs += expr.coeff;

                        model.addConstr(constr.lhs, constr.sense, constr.rhs, constr.constrName);
                    }
                    constr = new Constr();
                    constr.constrName = next.substring(0, next.length() - 1);
                    expr = new Expr();
                }
                else if (isLPComparator(next)) {
                    if (expr != null) {
                        constr.rhs = -expr.coeff;
                        expr = null;
                    }
                    constr.sense = next.charAt(0);
                }
                else if (tokenizer.isNumber()) {
                    if (expr == null) expr = new Expr();
                    if (next.length() == 1 && next.charAt(0) == '+') {
                        expr.coeff = 1.0;
                    }
                    else if (next.length() == 1 && next.charAt(0) == '-') {
                        expr.coeff = -1.0;
                    }
                    else {
                        expr.coeff = expr.coeff != null ? expr.coeff * new Double(next) : new Double(next);
                    }
                }
                else if (expr == null) {
                    section = Section.SubjectTo;
                    //tokenizer.undo();
                }
                else {
                    expr.varname = next;
                    if ((var = model.getVar(expr.varname)) == null)
                        var = model.addNumVar(0, Double.MAX_VALUE, 0, expr.varname);
                    constr.lhs.addTerm(expr.coeff == null ? 1 : expr.coeff, var);
                    expr = null;
                }
            }

            // reading bounds
            else if (section == Section.Bounds) {
                if (isLPKeyword(next)) {
                    section = null;
                    tokenizer.undo();
                }
                else {
                    tokenizer.undo();
                    char sense = '\0';
                    Double temp = null;
                    var = null;

                    while (tokenizer.hasToken()) {
                        next = tokenizer.nextToken();
                        if (tokenizer.isNumber()) {
                            temp = new Double(next);

                            // if var is known, number is at right
                            if (var != null) {
                                switch (sense) {
                                    case '<':
                                        var.setUB(temp);
                                        break;
                                    case '>':
                                        var.setLB(temp);
                                        break;
                                    case '=':
                                        var.setLB(temp);
                                        var.setUB(temp);
                                        break;
                                }
                            }
                        }
                        else if (isLPComparator(next)) {
                            sense = next.charAt(0);
                        }
                        else {
                            var = model.getVar(next);

                            // if there is a number on the left, then...
                            if (temp != null && sense != '\0') {
                                switch (sense) {
                                    case '<':
                                        var.setLB(temp);
                                        break;
                                    case '>':
                                        var.setUB(temp);
                                        break;
                                    case '=':
                                        var.setLB(temp);
                                        var.setUB(temp);
                                        break;
                                }
                            }
                        }
                    }
                }
            }

            // reading integers
            else if (section == Section.Integer) {
                if (isLPKeyword(next)) {
                    section = null;
                    tokenizer.undo();
                }
                else {
                    var = model.getVar(next);
                    var.setType(MPVar.INTEGER);
                }
            }

            // reading binaries
            else if (section == Section.Binaries) {
                if (isLPKeyword(next)) {
                    section = null;
                    tokenizer.undo();
                }
                else {
                    var = model.getVar(next);
                    var.setUB(1.0);
                    var.setType(MPVar.BINARY);
                }
            }

            // in case a keyword is detected...
            else if (section == null) {
                if ((section = sectionLP(next)) != null) {

                    // checking keywords to define current section
                    if (section == Section.Objective) {
                        model.getObjective().setDirection(next.contains("max") ? MPObjective.MAXIMIZE : MPObjective.MINIMIZE);
                        section = Section.Objective;
                    }
                    else if (section == Section.SubjectTo) {
                        while (tokenizer.hasToken()) {
                            next = tokenizer.nextToken();
                            if (!isLPKeyword(next)) {
                                tokenizer.undo();
                                break;
                            }
                        }
                    }
                }
            }
        }
        return model;
    }

    // TODO: implement MPS reader
    public MPModel loadMPS(BufferedReader reader) throws IOException {
        // creating the new model
        MPModel model = new MPModel();

        // auxiliary variables
        Constr constr = new Constr();
        Expr expr = new Expr();
        MPVar var;
        String next;
        Section section = Section.Objective;

        // reading first line
        String line = reader.readLine();
        LPTokenizer tokenizer = new LPTokenizer(line);

        while (tokenizer.hasToken() || (line = reader.readLine()) != null) {
            if (!tokenizer.hasToken()) tokenizer = new LPTokenizer(line);
            if (!tokenizer.hasToken()) continue;

            next = tokenizer.nextToken();

            // if line is a comment
            if (next != null && next.charAt(0) == '\\') {
                tokenizer.finish(); // skip the entire line since it corresponds to a comment
            }

            // reading objective function
            else if (section == Section.Objective) {
                if (isLPKeyword(next)) {
                    section = null;
                    tokenizer.undo();
                }
                else if (tokenizer.isNumber()) {
                    if (expr == null) expr = new Expr();
                    if (next.length() == 1 && next.charAt(0) == '+')
                        expr.coeff = 1.0;
                    else if (next.length() == 1 && next.charAt(0) == '-')
                        expr.coeff = -1.0;
                    else
                        expr.coeff = expr.coeff != null ? expr.coeff * new Double(next) : new Double(next);
                }
                else if (expr == null && next.contains(":")) {
                    section = Section.SubjectTo;
                    tokenizer.undo();
                }
                else if (expr != null) {
                    expr.varname = next;
                    model.addNumVar(0, Double.MAX_VALUE, expr.coeff, expr.varname);
                    expr = null;
                }
                else {
                    throw new Error("Unexpected word in LP file.");
                }
            }

            // reading constraints
            else if (section == Section.SubjectTo) {
                if (isLPKeyword(next)) {
                    section = null;
                    tokenizer.undo();
                }
                else if (next.endsWith(":")) {
                    if (constr.sense != '\0') {
                        if (expr != null)
                            constr.rhs += expr.coeff;

                        model.addConstr(constr.lhs, constr.sense, constr.rhs, constr.constrName);
                    }
                    constr = new Constr();
                    constr.constrName = next.substring(0, next.length() - 1);
                    expr = new Expr();
                }
                else if (isLPComparator(next)) {
                    if (expr != null) {
                        constr.rhs = -expr.coeff;
                        expr = null;
                    }
                    constr.sense = next.charAt(0);
                }
                else if (tokenizer.isNumber()) {
                    if (expr == null) expr = new Expr();
                    if (next.length() == 1 && next.charAt(0) == '+') {
                        expr.coeff = 1.0;
                    }
                    else if (next.length() == 1 && next.charAt(0) == '-') {
                        expr.coeff = -1.0;
                    }
                    else {
                        expr.coeff = expr.coeff != null ? expr.coeff * new Double(next) : new Double(next);
                    }
                }
                else if (expr == null) {
                    section = Section.SubjectTo;
                    //tokenizer.undo();
                }
                else {
                    expr.varname = next;
                    if ((var = model.getVar(expr.varname)) == null)
                        var = model.addNumVar(0, Double.MAX_VALUE, 0, expr.varname);
                    constr.lhs.addTerm(expr.coeff == null ? 1 : expr.coeff, var);
                    expr = null;
                }
            }

            // reading bounds
            else if (section == Section.Bounds) {
                if (isLPKeyword(next)) {
                    section = null;
                    tokenizer.undo();
                }
                else {
                    tokenizer.undo();
                    char sense = '\0';
                    Double temp = null;
                    var = null;

                    while (tokenizer.hasToken()) {
                        next = tokenizer.nextToken();
                        if (tokenizer.isNumber()) {
                            temp = new Double(next);

                            // if var is known, number is at right
                            if (var != null) {
                                switch (sense) {
                                    case '<':
                                        var.setUB(temp);
                                        break;
                                    case '>':
                                        var.setLB(temp);
                                        break;
                                    case '=':
                                        var.setLB(temp);
                                        var.setUB(temp);
                                        break;
                                }
                            }
                        }
                        else if (isLPComparator(next)) {
                            sense = next.charAt(0);
                        }
                        else {
                            var = model.getVar(next);

                            // if there is a number on the left, then...
                            if (temp != null && sense != '\0') {
                                switch (sense) {
                                    case '<':
                                        var.setLB(temp);
                                        break;
                                    case '>':
                                        var.setUB(temp);
                                        break;
                                    case '=':
                                        var.setLB(temp);
                                        var.setUB(temp);
                                        break;
                                }
                            }
                        }
                    }
                }
            }

            // reading integers
            else if (section == Section.Integer) {
                if (isLPKeyword(next)) {
                    section = null;
                    tokenizer.undo();
                }
                else {
                    var = model.getVar(next);
                    var.setType(MPVar.INTEGER);
                }
            }

            // reading binaries
            else if (section == Section.Binaries) {
                if (isLPKeyword(next)) {
                    section = null;
                    tokenizer.undo();
                }
                else {
                    var = model.getVar(next);
                    var.setUB(1.0);
                    var.setType(MPVar.BINARY);
                }
            }

            // in case a keyword is detected...
            else if (section == null) {
                if ((section = sectionLP(next)) != null) {

                    // checking keywords to define current section
                    if (section == Section.Objective) {
                        model.getObjective().setDirection(next.contains("max") ? MPObjective.MAXIMIZE : MPObjective.MINIMIZE);
                        section = Section.Objective;
                    }
                    else if (section == Section.SubjectTo) {
                        while (tokenizer.hasToken()) {
                            next = tokenizer.nextToken();
                            if (!isLPKeyword(next)) {
                                tokenizer.undo();
                                break;
                            }
                        }
                    }
                }
            }
        }
        return model;
    }

    private boolean isLPComparator(String word) {
        switch (word) {
            case "<":
            case ">":
            case "=":
            case "<=":
            case ">=":
                return true;

            default:
                return false;
        }
    }

    private boolean isLPKeyword(String word) {
        return word.startsWith("\\") || sectionLP(word) != null;
    }

    private Section sectionLP(String keyword) {
        switch (keyword.toLowerCase()) {
            case "min":
            case "min:":
            case "minimize":
            case "minimise":
            case "minimum":
            case "max":
            case "max:":
            case "maximize":
            case "maximise":
            case "maximum":
                return Section.Objective;

            case "subject":
            case "such":
            case "st":
            case "s.t.":
            case "st.":
            case "subjectto":
            case "suchthat":
            case "to":
            case "that":
            case "to:":
                return Section.SubjectTo;

            case "bounds":
                return Section.Bounds;

            case "generals":
            case "general":
            case "gens":
            case "gen":
            case "integers":
            case "integer":
            case "ints":
            case "int":
                return Section.Integer;

            case "binaries":
            case "binary":
            case "bins":
            case "bin":
                return Section.Binaries;

            case "end":
                return Section.End;

            default:
                return null;
        }
    }

    // MPS files are currently loaded using CPLEX or Gurobi's engine

    // TODO: implement MPS reader
    private boolean isMPSComparator(String word) {
        switch (word) {
            case "<":
            case ">":
            case "=":
            case "<=":
            case ">=":
                return true;

            default:
                return false;
        }
    }

    // TODO: implement MPS reader
    private boolean isMPSKeyword(String word) {
        return word.startsWith("\\") || sectionLP(word) != null;
    }

    // TODO: implement MPS reader
    private Section sectionMPS(String keyword) {
        switch (keyword.toLowerCase()) {
            case "min":
            case "min:":
            case "minimize":
            case "minimise":
            case "minimum":
            case "max":
            case "max:":
            case "maximize":
            case "maximise":
            case "maximum":
                return Section.Objective;

            case "subject":
            case "such":
            case "st":
            case "s.t.":
            case "st.":
            case "subjectto":
            case "suchthat":
            case "to":
            case "that":
            case "to:":
                return Section.SubjectTo;

            case "bounds":
                return Section.Bounds;

            case "generals":
            case "general":
            case "gens":
            case "gen":
            case "integers":
            case "integer":
            case "ints":
            case "int":
                return Section.Integer;

            case "binaries":
            case "binary":
            case "bins":
            case "bin":
                return Section.Binaries;

            case "end":
                return Section.End;

            default:
                return null;
        }
    }


    private class Constr {

        public String constrName = null;
        public MPLinExpr lhs = new MPLinExpr();
        public double rhs = 0;
        public char sense = '\0';
    }

    private class Expr {

        public Double coeff = null;
        public String varname = null;
    }

    private enum Section {
        Objective, SubjectTo, Bounds, Integer, Binaries, Any, End
    }
}

# JADS

## Java automatic decomposition-based solver

Written by Túlio Toffolo.

The JADS is a framework for implementing decomposition-based algorithms.
It may also be used as a standalone solved, and additional functionalities are being implemented for that.
The current version was adapted and evaluated considering four different problems, obtaining state-of-the-art results.

Please address all contributions, suggestions, and inquiries to the current project administrator via e-mail to jads@toffolo.com.br.

## Documentation

The code's documentation is currently being improved.
Videos and tutorials will be available online within the next months, with examples and use-cases of the framework.

## Maintenance

The framework is currently maintained by Túlio Toffolo, and additional functionalities will soon be available, including a friendly user interface for creating input files and executing the solver.

## Compiling the code

The JADS is currently a framework which requires a Mixed Integer Programming (MIP) solver to be executed.
It is possible to compile the code using the provided opaque structures which replace JAR files of the supported solvers (CPLEX, Gurobi and SCIP).
Note, however, that the provided JAR files will result in runtime errors.

This project uses gradle. To generate the binaries, just run:

- gradle build

The library file (jads.jar) will be generated.

## Usage

Decomposition-based heuristic algorithms:

```
Usage: ./jads.jar <input_file> <output_file> [options]

    <input_file>  : path of input jdec file.
    <output_file> : path of the output (solution) file.

Options:

    -sol <sol_file>    : Initial solution (default = NULL).

    -seed <int>        : Random seed (default = 2).
    -timelimit <double>: Time limit in minutes (default = INF).

    -cbc               : Use CBC solver.
    -cplex             : Use Cplex solver.
    -gurobi            : Use Gurobi solver (default).
    -scip              : Use SCIP solver.

    -sollimit <int>    : Solution limit (default = INF).
    -subtlimit <double>: Time limit in minutes for each subproblem (default = INF).
```

Dantzig-Wolfe' decomposition:

```
Usage: ./acg.jar <input_file> <output_file> [options]

    <input_file>   : path of input model.
    <output_file>  : path of the output (solution) file.

Options:

    -cplex             : Use Cplex simplex and MIP solver.
    -gurobi            : Use Gurobi simplex and MIP solver (default).

    -sollimit <int>    : Solution limit (default = INT_MAX).
    -timelimit <int>   : Time limit in secs for each pricing (default = INT_MAX).
    -timemult <double> : Time multiplier (default = 2.0).
    -populate          : Populate (default = false).
    -numpopulate <int> : Populate solutions limit (default = 20).

    -onepricing        : Resolve the master after every pricing with new columns.
```

## Known issues

SCIP compatibility is currently very limited, and therefore we recommend using CPLEX or Gurobi.
As with the remaining of the framework, compatibility with SCIP is being improved.

## Requirements

Java 1.8 is required.
The decomposition-based heuristics require CPLEX, Gurobi or SCIP.
Make sure either cplex.jar, gurobi.jar or scip.jar is on your classpath before executing the code.

## Questions?

If you have any questions, please more than feel free to contact me.

Thanks!

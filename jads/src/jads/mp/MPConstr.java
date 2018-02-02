package jads.mp;

/**
 * This interface represents a Constraint of the model.
 *
 * @author Tulio Toffolo
 */
public interface MPConstr {

    /**
     * Gets the index of the constraint
     *
     * @return the index of the constraint
     */
    int getIndex();

    /**
     * Gets the name of the constraint.
     *
     * @return the name of the constraint
     */
    String getName();

    /**
     * Gets the sense of the constraint ('&lt;', '&gt;' or '=').
     *
     * @return the char representing the sense of the constraint
     */
    char getSense();
}

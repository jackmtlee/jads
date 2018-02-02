package jads.mp;

/**
 * This interface provides the method of a Loader class, used to read
 * formulation from files.
 *
 * @author Tulio Toffolo
 */
public interface MPLoader {

    /**
     * Loads a formulation from a file.
     *
     * @param filePath the file path
     * @return the loaded formulation
     */
    MPModel loadModel(String filePath);
}

package ab.demo;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;

/**
 * General utility methods and fields for the AssignmentAgent
 * Runnable.
 */
public class AssignmentAgentUtils {

    /**
     * The name of the directory to write learning data to.
     */
    public static final String DATA_DIRECTORY = "learnings";

    /**
     * The name of the directory to write temporary data to.
     */
    public static final String TMP_DIRECTORY = "tmp";

    /**
     * Appends a line to a given file. Used to append to
     * ARFF files.
     *
     * @param file the file to write to.
     * @param line the line to append.
     */
    public static void appendFile(File file, String line) throws IOException {
        BufferedWriter writer = new BufferedWriter(new FileWriter(file, true));

        writer.write(line + "\n");
        writer.flush();
        writer.close();
    }

    /**
     * Creates a level-specific file for the provided
     * level value.
     *
     * @param level the level to create for.
     * @return the created File.
     */
    public static File createLevelFile(int level){
        return createFile("learnings", level);
    }

    /**
     * Creates a tmp-specific file for the provided
     * level value.
     *
     * @param level the level to create for.
     * @return the created File.
     */
    public static File createTempFile(int level){
        return createFile("tmp", level);
    }

    /**
     * Creates a file for the provided level value inside
     * the provided folder name.
     *
     * @param folder the name of the folder.
     * @param level the level to create for.
     * @return the created File.
     */
    public static File createFile(String folder, int level) {
        return new File("./" + folder + "/level" + level + ".arff");
    }

}

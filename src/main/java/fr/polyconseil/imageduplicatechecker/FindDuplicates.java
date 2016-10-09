package fr.polyconseil.imageduplicatechecker;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.math.BigInteger;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import hudson.model.TaskListener;

import org.apache.commons.io.FilenameUtils;

/*
 * an amalgamation of the memory hungry "find duplicate files" program from here ...
 * https://jakut.is/2011/03/15/a-java-program-to-list-all/
 * with the space economic hashing code found here ...
 * http://stackoverflow.com/questions/1741545/java-calculate-sha-256-hash-of-large-file-efficiently
 */
public class FindDuplicates {
    private static MessageDigest md;
    private static final String NOT_ALLOWED_FILENAME = "staticassets";
    private static final List<String> ALLOWED_FILE_EXTENTIONS = Arrays.asList("png","jpg");
    static {
        try {
            md = MessageDigest.getInstance("SHA-512");
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("cannot initialize SHA-512 hash function", e);
        }
    }

    public static void find(Map<String, List<String>> duplicatesMap, File directory, boolean leanAlgorithm) throws Exception  {
        for (File child : directory.listFiles()) {
        if (child.getPath().contains(NOT_ALLOWED_FILENAME)) {
        return;
        } else if (child.isDirectory()) {
                find(duplicatesMap, child, leanAlgorithm);
            } else {
                try {
                String extension = FilenameUtils.getExtension(child.getName());
                if (ALLOWED_FILE_EXTENTIONS.contains(extension)) {
                    String hash = leanAlgorithm ? makeHashLean(child) : makeHashQuick(child);
                    List<String> filenameList = duplicatesMap.get(hash);
                    if (filenameList == null) {
                        filenameList = new LinkedList<String>();
                        duplicatesMap.put(hash, filenameList);
                    }
                    filenameList.add(child.getAbsolutePath());
                }
                } catch (IOException e) {
                    throw new RuntimeException("cannot read file " + child.getAbsolutePath(), e);
                }
            }
        }
    }

    /*
     * quick but memory hungry (might like to run with java -Xmx2G or the like to increase heap space if RAM available)
     */
    public static String makeHashQuick(File infile) throws Exception {
        FileInputStream fin = new FileInputStream(infile);
        byte data[] = new byte[(int) infile.length()];
        fin.read(data);
        fin.close();
        String hash = new BigInteger(1, md.digest(data)).toString(16);
        return hash;
    }

    /*
     * slower but memory efficient  -- you might like to play with the size defined by "buffSize"
     */
    public static String makeHashLean(File infile) throws Exception {
        RandomAccessFile file = new RandomAccessFile(infile, "r");

        int buffSize = 16384;
        byte[] buffer = new byte[buffSize];
        long read = 0;

        // calculate the hash of the whole file for the test
        long offset = file.length();
        int unitsize;
        while (read < offset) {
            unitsize = (int) (((offset - read) >= buffSize) ? buffSize
: (offset - read));
            file.read(buffer, 0, unitsize);
            md.update(buffer, 0, unitsize);
            read += unitsize;
        }

        file.close();
        String hash = new BigInteger(1, md.digest()).toString(16);
        return hash;
    }

    //List<String> args1 = new ArrayList<String>();
    //args1.add("/Users/wzhang/dev/autoslave/autoslave");
    public static void execute(String str, TaskListener listener) {
    	//String str = "/Users/weiweizhang/Documents/workspace/images_checker_test_folders";
        if (str.isEmpty()) {
            System.out.println("Please supply a path to directory to find duplicate files in.");
            return;
        }
        File dir = new File(str);
        if (!dir.isDirectory()) {
            System.out.println("Supplied directory (" + str  + ") does not exist.");
            return;
        }
        Map<String, List<String>> duplicatesMap = new HashMap<String, List<String>>();
        try {
            FindDuplicates.find(duplicatesMap, dir, true);
        } catch (Exception e) {
            e.printStackTrace();
        }
        for (List<String> filenameList : duplicatesMap.values()) {
            if (filenameList.size() > 1) {
                listener.getLogger().println("Found results:");
                for (String filename : filenameList) {
                    System.out.println(filename);
                    listener.getLogger().println(filename);
                }
                System.out.println("--");
            }
        }
    }
}
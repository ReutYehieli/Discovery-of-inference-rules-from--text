//import org.tartarus.snowball2.runStemmer;
import tartarus.snowball2.ext.runStemmer;
import java.io.*;
import java.util.LinkedList;

class TestSetAux {
    private static String englishStem(String[] str){
        String output = "";
        for (String s : runStemmer.englishStem(str, 1)){
            output = output.concat(s + " ");
        }
        return output.trim();
    }


    private static String getStemmedPath(String path){
        String root = path.substring(2, path.length() - 2);
        return englishStem(new String[]{root});
    }

    private static BufferedReader getBufferedReader(String testSetPath) throws IOException {
//        String filePath = "/home/hadoop/" + testSetPath;
        String filePath = "negTestSet";
        File file = new File(filePath);
        return new BufferedReader(new FileReader(file));
    }

    static LinkedList<String> getTestSetPaths(String testSetPath) throws IOException {
        LinkedList<String> testSetPaths = new LinkedList<>();
        BufferedReader br = getBufferedReader(testSetPath);
        String line;
        while ((line = br.readLine()) != null){
            String[] splitted = line.split("\t");
            String first = getStemmedPath(splitted[0]);
            String second = getStemmedPath(splitted[1]);
            if (!testSetPaths.contains(first)) testSetPaths.add(first);
            if (!testSetPaths.contains(second)) testSetPaths.add(second);
        }
        return testSetPaths;
    }

    static LinkedList<String> getTestSetPathPairs(String testSetPath) throws IOException {
        LinkedList<String> testSetPairs = new LinkedList<>();
        BufferedReader br = getBufferedReader(testSetPath);
        String line;
        while ((line = br.readLine()) != null) {
            String[] splitted = line.split("\t");
            String firstSecond = getStemmedPath(splitted[0]) + "/" + getStemmedPath(splitted[1]);
            testSetPairs.add(firstSecond);
        }
        return testSetPairs;
    }

    static boolean bothEndsWithSameChar (String first, String second){
        return (first.endsWith("$") && second.endsWith("$")) || (first.endsWith("%") && second.endsWith("%"));
    }
}
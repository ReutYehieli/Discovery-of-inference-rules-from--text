package tartarus.snowball2.ext;

//import org.tartarus.snowball2.ext.englishStemmer;

public class runStemmer {
    public static String[] englishStem(String[] results, int length){
        String[] output = new String[length];
        for (int i = 0; i < results.length; i++){
            englishStemmer es = new englishStemmer();
            String[] splitted = results[i].split(" ");
            String curr = "";
            for(String word : splitted){
                es.setCurrent(word);
                es.stem();
                curr = curr + " " + es.getCurrent();
            }
            output[i] = curr.trim();
        }
        return output;
    }
}
import org.apache.hadoop.io.LongWritable;
import org.apache.hadoop.io.Text;
import org.apache.hadoop.mapreduce.Mapper;
import org.apache.hadoop.mapreduce.Partitioner;
import org.apache.hadoop.mapreduce.Reducer;
import org.tartarus.snowball.ext.EnglishStemmer;

import java.io.IOException;
import java.util.LinkedList;
public class DetailsNum {
    public static class MapperClassForNum extends Mapper<LongWritable, Text, Text, Text> {
        private LinkedList<String> testPathsInPairs = new LinkedList<>();
        private static String englishStem(String[] str){
            String output = "";
            String[] eng1_output = new String[1];
            for (int i = 0; i < str.length; i++){
                EnglishStemmer es = new EnglishStemmer();
                String[] splitted = str[i].split(" ");
                String curr = "";
                for(String word : splitted){
                    es.setCurrent(word);
                    es.stem();
                    curr = curr + " " + es.getCurrent();
                }
                eng1_output[i] = curr.trim();
            }
            return output.trim();
        }
        public void setup(Context context)  throws IOException, InterruptedException {
            String testPathString=  context.getConfiguration().get("testPathString");
            try{
                for(String path : testPathString.split("\n")){
                    String [] p = path.split("\t");
                    String pair = englishStem(new String[]{p[0]}) + "/" + englishStem(new String[]{p[1]});
                    testPathsInPairs.add(pair);
                }
            } catch (Exception e){
                e.printStackTrace();
            }
        }

        public void map(LongWritable lineId, Text line, Context context) throws IOException,  InterruptedException {
            String [] lineInput = line.toString().split("\t");
            for (int i = 1; i < lineInput.length; i ++){
                String [] first = lineInput[i].split("/");
                double firstMi = Double.parseDouble(first[1]);
                for (int j = 1; j < lineInput.length; j++){
                    if (i != j){
                        String [] second = lineInput[j].split("/");
                        double misSum = Double.parseDouble(second[1]) + firstMi;
                        String pairToCheck = first[0].substring(0, first[0].length() - 1) + "/" +
                                second[0].substring(0, second[0].length() - 1);
                        char symbolF = first[0].charAt(first[0].length()-1);
                        char symbolS = first[1].charAt(first[1].length()-1);
                        boolean sameSlot = (symbolF == symbolS) && (symbolS == '%'  || symbolS =='$');
                        if (testPathsInPairs.contains (pairToCheck) && sameSlot) {
                            context.write(new Text(first[0] + "/" + second[0]), new Text(Double.toString(misSum)));
                        }
                    }
                }
            }
        }
        @Override
        public void cleanup(Context context)  throws IOException, InterruptedException {
        }

    }
    public static class ReducerClass extends Reducer<Text,Text,Text, Text> {
        @Override
        public void setup(Context context)  throws IOException, InterruptedException {
        }

        @Override
        public void reduce(Text key, Iterable<Text> values, Context context) throws IOException,  InterruptedException {
            double sum = 0.0;
            for (Text value : values){
                sum += Double.parseDouble(value.toString());
            }
            context.write(key, new Text(Double.toString(sum)));
        }

        @Override
        public void cleanup(Context context)  throws IOException, InterruptedException {
        }
    }
    public static class PartitionerClass extends Partitioner<Text, Text> {

        @Override
        public int getPartition(Text key, Text value, int numPartitions) {
            return (key.hashCode() & Integer.MAX_VALUE) % numPartitions;
        }

    }
}

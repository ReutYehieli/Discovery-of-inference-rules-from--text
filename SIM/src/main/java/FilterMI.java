import org.apache.hadoop.io.LongWritable;
import org.apache.hadoop.io.Text;
import org.apache.hadoop.mapreduce.Mapper;
import org.apache.hadoop.mapreduce.Reducer;
import org.apache.hadoop.mapreduce.Partitioner;

import java.io.*;
import java.util.HashSet;
import java.util.LinkedList;
import java.io.IOException;
import org.tartarus.snowball.ext.EnglishStemmer;
public class FilterMI {
    public static class MapperClass extends Mapper<LongWritable, Text, Text, Text> {

        //private LinkedList<String> testSetPaths;
        private HashSet<String> testPaths;

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



        @Override
        public void setup(Context context)  throws IOException {
            String testPathString=  context.getConfiguration().get("testPathString");
            try{
                for(String path : testPathString.split("\n")){
                    String [] p = path.split("\t");
                    String firstP =  englishStem(new String[] {p[0].substring(2,p[0].length()-2)});
                    String secondP =  englishStem(new String[] {p[1].substring(2,p[1].length()-2)});
                    if(!testPaths.contains(firstP)) testPaths.add(firstP);
                    if(!testPaths.contains(secondP)) testPaths.add(secondP);
                }
            } catch (Exception e){
                e.printStackTrace();
            }
        }

        @Override
        public void map(LongWritable lineId, Text line, Context context) throws IOException,  InterruptedException {
            String [] lineInput = line.toString().split("\t");
            String [] path = lineInput[0].split("/");
            String innperPath = path[0];
            if (testPaths.contains(innperPath)){
                context.write(new Text(lineInput[0]), new Text(lineInput[1]));
            }
        }

        @Override
        public void cleanup(Context context) {
        }

    }


    public static class ReducerClass extends Reducer<Text,Text,Text, Text> {
        @Override
        public void setup(Context context) {
        }

        @Override
        public void reduce(Text key, Iterable<Text> values, Context context) throws IOException,  InterruptedException {
            for (Text value : values)
                context.write(key, value);
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



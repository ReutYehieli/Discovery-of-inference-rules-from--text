import org.apache.hadoop.io.LongWritable;
import org.apache.hadoop.io.Text;
import org.apache.hadoop.mapreduce.Mapper;
import org.apache.hadoop.mapreduce.Partitioner;
import org.apache.hadoop.mapreduce.Reducer;

import java.io.IOException;
import java.util.LinkedList;
import java.util.TreeMap;

public class SimFile {
    public static class MapperClassPairs extends Mapper<LongWritable, Text, Text, Text> {
        @Override
        public void setup(Context context)  throws IOException, InterruptedException {
        }

        @Override
        public void map(LongWritable lineId, Text line, Context context) throws IOException,  InterruptedException {
            String [] splitted = line.toString().split("\t");
            context.write(new Text(splitted[0]), new Text(splitted[1] + "\tnum"));
        }

        @Override
        public void cleanup(Context context)  throws IOException, InterruptedException {
        }

    }
    public static class MapperClassPaths extends Mapper<LongWritable, Text,Text, Text> {
        private TreeMap<String, String> pathsMis;
        private LinkedList<String> testSetPairs;
        private String testSetPath;
        @Override
        public void setup(Context context)  throws IOException, InterruptedException {
            pathsMis = new TreeMap<>();
            testSetPath = context.getConfiguration().get("testPathString");
            testSetPairs = TestSetAux.getTestSetPathPairs(testSetPath);
        }

        @Override
        public void map(LongWritable lineId, Text line, Context context) throws IOException,  InterruptedException {
            String [] splitted = line.toString().split("\t");
            pathsMis.put(splitted[0], splitted[1]);
        }

        @Override
        public void cleanup(Context context)  throws IOException, InterruptedException {
            int size = pathsMis.size();
            String[] pathKeyArr = pathsMis.keySet().toArray(new String[size]);
            for (int i = 0; i < size; i ++){
                String first = pathKeyArr[i];
                Double firstMi = Double.parseDouble(pathsMis.get(first));
                for (int j = 0; j < size ; j ++){
                    if (j != i){
                        String second = pathKeyArr[j];
                        if (TestSetAux.bothEndsWithSameChar(first, second) &&
                                testSetPairs.contains(first.substring(0, first.length() - 1) + "/" +
                                        second.substring(0, second.length() - 1))) {
                            Double totalMi = Double.parseDouble(pathsMis.get(second)) + firstMi;
                            String key = first + "/" + second;
                            context.write(new Text(key), new Text(totalMi.toString()));
                        }
                    }
                }
            }
        }

    }


    public static class ReducerClass extends Reducer<Text,Text,Text, Text> {
        @Override
        public void setup(Context context)  throws IOException, InterruptedException {
        }

        @Override
        public void reduce(Text key, Iterable<Text> values, Context context) throws IOException,  InterruptedException {
            LinkedList<String> inputs = new LinkedList<>();
            for (Text value : values){
                inputs.add(value.toString());
            }
            if (inputs.size() == 2){
                Double den = 0.0, num = 0.0;
                for (String str : inputs){
                    String [] splitted = str.split("\t");
                    if (splitted.length == 2){
                        num = Double.parseDouble(splitted[0]);
                    }
                    else{
                        den = Double.parseDouble(splitted[0]);
                    }
                }
                if (den != 0.0 && num != 0.0) {
                    String [] slashSplitted = key.toString().split("/");
                    String newKey = slashSplitted[0].substring(0, slashSplitted[0].length() - 1) + "/" +
                            slashSplitted[1].substring(0, slashSplitted[1].length() - 1) + "\t" +
                            slashSplitted[1].charAt(slashSplitted[1].length() - 1);
                    context.write(new Text(newKey), new Text(Double.toString(num / den)));
                }
            }
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

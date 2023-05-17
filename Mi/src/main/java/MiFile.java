import org.apache.hadoop.io.LongWritable;
import org.apache.hadoop.io.Text;
import org.apache.hadoop.mapreduce.Mapper;
import org.apache.hadoop.mapreduce.Partitioner;
import org.apache.hadoop.mapreduce.Reducer;

import java.io.IOException;
import java.util.LinkedList;

public class MiFile {
    public static class MapperClassJoinedPath extends Mapper<LongWritable, Text, Text, Text> {
        @Override
        public void setup(Context context)  throws IOException, InterruptedException {
        }

        @Override
        public void map(LongWritable lineId, Text line, Context context) throws IOException,  InterruptedException {
            String[] splitted = line.toString().split("\t");
            context.write(new Text(splitted[0]), new Text(splitted[1] + "\t" + splitted[2]));
        }

        @Override
        public void cleanup(Context context)  throws IOException, InterruptedException {
        }

    }
    public static class MapperClassSlotSums extends Mapper<LongWritable, Text,Text, Text> {
        @Override
        public void setup(Context context)  throws IOException, InterruptedException {
        }

        @Override
        public void map(LongWritable lineId, Text line, Context context) throws IOException,  InterruptedException {
            String[] splitted = line.toString().split("\t");
            context.write(new Text(splitted[0]), new Text(splitted[1]));
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
            LinkedList<String> paths = new LinkedList<>();
            LinkedList<Double> sums = new LinkedList<>();
            long den = -1;
            for (Text value : values){
                String[] splitted = value.toString().split("\t");
                if (splitted.length == 1){
                    den = Long.parseLong(splitted[0]);
                }
                else{
                    paths.addLast(splitted[0]);
                    sums.addLast(Double.parseDouble(splitted[1]));
                }
            }
            if (den > 0) {
                while (!sums.isEmpty()) {
                    double sum = (sums.removeFirst() * 1.0) / den;
                    String path = paths.removeFirst();
                    if (sum >= 1) {
                        double result = Math.log10(sum) / Math.log10(2.0);
                        String keyStr = key.toString();

                        if (keyStr.endsWith("$")){
                            context.write(new Text(path + "/x/" + keyStr.substring(0, keyStr.length() - 1)),
                                    new Text(Double.toString(result)));
                        }
                        else{ // ends with %
                            context.write(new Text(path + "/y/" + keyStr.substring(0, keyStr.length() - 1)),
                                    new Text(Double.toString(result)));
                        }

                    }
                }
            }
            paths.clear();
            sums.clear();

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

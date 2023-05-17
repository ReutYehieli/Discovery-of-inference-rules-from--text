import org.apache.hadoop.io.LongWritable;
import org.apache.hadoop.io.Text;
import org.apache.hadoop.mapreduce.Mapper;
import org.apache.hadoop.mapreduce.Partitioner;
import org.apache.hadoop.mapreduce.Reducer;
import java.io.IOException;
import java.util.HashMap;


public class JoinPathAndPathSlot {
    public static class MapperClassPath extends Mapper<LongWritable, Text,Text, Text> {

        @Override
        public void setup(Context context)  throws IOException, InterruptedException {
        }

        @Override
        public void map(LongWritable lineId, Text line, Context context) throws IOException,  InterruptedException {
            String[] lineInput = line.toString().split("\t");
            //<Path,numberOfOcc>
            context.write(new Text(lineInput[0]), new Text(lineInput[1]));
        }

        @Override
        public void cleanup(Context context)  throws IOException, InterruptedException {
        }

    }

    public static class MapperClassPathSlot extends Mapper<LongWritable, Text,Text, Text> {

        @Override
        public void setup(Context context) throws IOException, InterruptedException {
        }

        @Override
        public void map(LongWritable lineId, Text line, Context context) throws IOException, InterruptedException {
            String[] lineInput = line.toString().split("\t");
            //<Path><Slot,numberOfOcc>
            context.write(new Text(lineInput[0]), new Text(lineInput[1] + "\t" + lineInput[2]));
        }

        @Override
        public void cleanup(Context context) throws IOException, InterruptedException {
        }
    }

    public static class ReducerClass extends Reducer<Text,Text,Text, Text> {
        private long N;
        private HashMap<String,Long> hm;

        @Override
        public void setup(Context context)  throws IOException, InterruptedException {
            N = context.getConfiguration().getLong("N", -1);
            hm =  new HashMap<>();
        }
        @Override
        public void reduce(Text key, Iterable<Text> values, Context context) throws IOException,  InterruptedException {
            long PathSlot = -1;
            for (Text value : values){
                String[] inputLine = value.toString().split("\t");
                if (inputLine.length == 1){
                    PathSlot = Long.parseLong(inputLine[0]);
                }
                else{
                    hm.put(inputLine[0], Long.parseLong(inputLine[1]));
                }
            }
            if (PathSlot > 0 ){
                for(String wordSlot : hm.keySet() ){
                   Long sum = hm.get(wordSlot);
                   double result = (N * sum * 1.0) / PathSlot;
                    context.write(new Text(wordSlot), new Text(key.toString() + "\t" + result));
                }
            }
            hm.clear();
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

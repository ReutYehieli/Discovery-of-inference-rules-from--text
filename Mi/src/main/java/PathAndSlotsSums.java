import org.apache.hadoop.io.LongWritable;
import org.apache.hadoop.io.Text;
import org.apache.hadoop.mapreduce.Mapper;
import org.apache.hadoop.mapreduce.Partitioner;
import org.apache.hadoop.mapreduce.Reducer;

import java.io.IOException;

public class PathAndSlotsSums {
    public static class MapperClass extends Mapper<LongWritable, Text, Text, Text> {
        private int index;

        @Override
        public void setup(Context context)  throws IOException, InterruptedException {
            index = context.getConfiguration().getInt("index", -1);
        }

        @Override
        public void map(LongWritable lineId, Text line, Context context) throws IOException,  InterruptedException {
            if (index != -1) {
                String[] inputLine = line.toString().split("\t");
                if (index == 0){
                    //<Path><numberOfOcc>
                    context.write(new Text(inputLine[0]), new Text(inputLine[3]));
                }
                else if (index == 1){
                    //<SlotX = Noun1><numberOfOcc>
                    //<SLOTY = Noun2><numberOfOcc>
                    context.write(new Text(inputLine[1]), new Text(inputLine[3]));
                    context.write(new Text(inputLine[2]), new Text(inputLine[3]));
                }
                else{
                    context.write(new Text(inputLine[0] + "\t" + inputLine[1]),
                            new Text((inputLine[3])));
                    context.write(new Text(inputLine[0] + "\t" + inputLine[2]),
                            new Text((inputLine[3])));
                }

            }
            else {
                System.out.println("The index is -1");
            }
        }

        @Override
        public void cleanup(Context context)  throws IOException, InterruptedException {
        }
    }
    public static class ReducerClass extends Reducer<Text, Text,Text,Text> {
        //private int index;
        @Override
        public void setup(Context context)  throws IOException, InterruptedException {
       //     index = context.getConfiguration().getInt("index", 3);
        }

        @Override
        public void reduce(Text path, Iterable<Text> values, Context context) throws IOException, InterruptedException {
                long count = 0;
                for (Text value : values) {
                    count = count + Long.parseLong(value.toString());
                }
                context.write(path, new Text(Long.toString(count)));
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

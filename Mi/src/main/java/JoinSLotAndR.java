import com.amazonaws.services.lexmodelbuilding.model.Slot;
import org.apache.hadoop.io.LongWritable;
import org.apache.hadoop.io.Text;
import org.apache.hadoop.mapreduce.Mapper;
import org.apache.hadoop.mapreduce.Partitioner;
import org.apache.hadoop.mapreduce.Reducer;
import java.io.IOException;
import java.util.LinkedList;

public class JoinSLotAndR {
    public static class MapperClassSLots extends Mapper<LongWritable, Text, Text, Text> {
        @Override
        public void setup(Context context) throws IOException, InterruptedException {
        }

        @Override
        public void map(LongWritable lineId, Text line, Context context) throws IOException, InterruptedException {
            String[] SlotsInput = line.toString().split("\t");
            context.write(new Text(SlotsInput[0]), new Text(SlotsInput[1]));
        }

        @Override
        public void cleanup(Context context) throws IOException, InterruptedException {
        }
    }

    public static class MapperClassResultFromJoinPath extends Mapper<LongWritable, Text, Text, Text> {
        @Override
        public void setup(Context context) throws IOException, InterruptedException {
        }

        @Override
        public void map(LongWritable lineId, Text line, Context context) throws IOException, InterruptedException {
            String[] SlotsInput = line.toString().split("\t");
            context.write(new Text(SlotsInput[0]), new Text(SlotsInput[1] + "\t" + SlotsInput[2]));
        }

        @Override
        public void cleanup(Context context) throws IOException, InterruptedException {
        }
    }

    public static class ReducerClass extends Reducer<Text, Text, Text, Text> {
        private LinkedList<String> Paths;
        private LinkedList<Double> sums;
        private long denominator;

        public void setup(Context context) throws IOException, InterruptedException {
            Paths = new LinkedList<>();
            sums = new LinkedList<>();
            denominator = -1;
        }

        @Override
        public void reduce(Text key, Iterable<Text> values, Context context) throws IOException, InterruptedException {
            for (Text value : values) {
                String[] inputLine = value.toString().split("\t");
                if (inputLine.length == 1) {  // we are in the case of < Slot, numberOfOcc>
                    denominator = Long.parseLong(inputLine[0]);
                } else {  //we are in the case of <<Slot>,<innerPath, result (fromthe pervios job)>>
                    Paths.addFirst(inputLine[1]);
                    sums.addFirst(Double.parseDouble(inputLine[2]));
                }
            }
                if (denominator > 0) {
                    while (!Paths.isEmpty()) {
                        double result = (sums.removeLast() * 1.0) / denominator;
                        String innerPath = Paths.removeLast();
                        if (result > 0) {
                            double finalResult = CalMi(result);
                            char symbol = key.toString().charAt(key.toString().length());
                            if (symbol == '$') // X _ Y
                            {
                                context.write(new Text(innerPath + "/x/" + key.toString().substring(0, key.toString().length() - 1)), new Text(Double.toString((finalResult))));
                                new Text(Double.toString((finalResult)));
                            } else {  //Y _ X -> end with % (SLOT Y)
                                context.write(new Text(innerPath + "/y/" + key.toString().substring(0, key.toString().length() - 1)), new Text(Double.toString((finalResult))));

                            }
                        }

                    }
                }
            Paths.clear();
            sums.clear();
            denominator = -1;
            }



        @Override
        public void cleanup(Context context)  throws IOException, InterruptedException {
        }

        public double CalMi(double number){
            return Math.log10(number) / Math.log10(2.0);
        }
    }


    public static class PartitionerClass extends Partitioner<Text, Text> {

        @Override
        public int getPartition(Text key, Text value, int numPartitions) {
            return (key.hashCode() & Integer.MAX_VALUE) % numPartitions;
        }

    }
}


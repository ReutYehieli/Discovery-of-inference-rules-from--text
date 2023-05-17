import org.apache.hadoop.io.LongWritable;
import org.apache.hadoop.io.Text;
import org.apache.hadoop.mapreduce.Mapper;
import org.apache.hadoop.mapreduce.Partitioner;
import org.apache.hadoop.mapreduce.Reducer;

import java.io.IOException;
import java.util.HashMap;
import java.util.LinkedList;

public class CountForWordAllPaths {
    public static class MapperClass extends Mapper<LongWritable, Text, Text, Text> {

            @Override
            public void setup(Context context)  throws IOException, InterruptedException {
            }

            @Override
            public void map(LongWritable lineId, Text line, Context context) throws IOException,  InterruptedException {
                String [] lineInput = line.toString().split("\t");
                String [] path = lineInput[0].split("/");
                String innerPath = path[0];
                if (path[1].equals("x")){
                    context.write(new Text(path[2]+"$"), new Text(innerPath +
                            "$\t" + lineInput[1]));
                }
                else{ //equals("y")
                    context.write(new Text(path[2]+"%"), new Text(innerPath +
                            "%\t" + lineInput[1]));
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
                String output = "";
                HashMap<String, Double> pathCounts = new HashMap<>();
                for (Text value : values){
                    String [] splitted = value.toString().split("\t");
                    if (pathCounts.containsKey(splitted[0])){
                        pathCounts.put(splitted[0],  pathCounts.get(splitted[0]) + Double.parseDouble(splitted[1]));
                    }
                    else{
                        pathCounts.put(splitted[0], Double.parseDouble(splitted[1]));
                    }

                }
                if (pathCounts.size() > 1) {
                    for (String path : pathCounts.keySet()) {
                        output = output.concat(path + "/" + pathCounts.get(path) + "\t");
                    }
                    output = output.substring(0, output.length() - 1);
                    context.write(key, new Text(output));
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




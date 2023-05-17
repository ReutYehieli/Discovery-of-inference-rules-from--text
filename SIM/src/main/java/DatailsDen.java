import org.apache.hadoop.io.LongWritable;
import org.apache.hadoop.io.Text;
import org.apache.hadoop.mapreduce.Mapper;
import org.apache.hadoop.mapreduce.Partitioner;
import org.apache.hadoop.mapreduce.Reducer;
import org.tartarus.snowball.ext.EnglishStemmer;

import java.io.IOException;
import java.util.LinkedList;
public class DatailsDen {
    public static class MapperClass extends Mapper<LongWritable, Text, Text, Text> {
    @Override
    public void setup(Context context)  throws IOException, InterruptedException {
    }

    @Override
    public void map(LongWritable lineId, Text line, Context context) throws IOException,  InterruptedException {
        String [] splitted = line.toString().split("\t");
        String [] slashSplitted = splitted[0].split("/");
        String path = slashSplitted[0];
        if (slashSplitted[1].equals("x")){
            context.write(new Text(path + "$"), new Text(splitted[1]));
        }
        else{ //equals y)
            context.write(new Text(path + "%"), new Text(splitted[1]));
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

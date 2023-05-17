
import org.apache.hadoop.fs.FileSystem;
import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.io.Text;
import org.apache.hadoop.mapreduce.Counter;
import org.apache.hadoop.mapreduce.Counters;
import org.apache.hadoop.mapreduce.Job;
import org.apache.hadoop.mapreduce.lib.input.FileInputFormat;
import org.apache.hadoop.mapreduce.lib.input.MultipleInputs;
import org.apache.hadoop.mapreduce.lib.input.SequenceFileInputFormat;
import org.apache.hadoop.mapreduce.lib.input.TextInputFormat;
import org.apache.hadoop.mapreduce.lib.jobcontrol.ControlledJob;
import org.apache.hadoop.mapreduce.lib.jobcontrol.JobControl;
import org.apache.hadoop.mapreduce.lib.output.FileOutputFormat;
import org.apache.hadoop.mapreduce.lib.output.TextOutputFormat;
import org.apache.hadoop.util.GenericOptionsParser;
import org.apache.log4j.BasicConfigurator;

import java.io.IOException;
import java.util.Arrays;
import java.util.LinkedList;


public class Main  {


    public static void main(String[] args) throws IOException, InterruptedException, ClassNotFoundException {
        LinkedList<String> pathsOfOutput = new LinkedList<>();
      //  String inputFile = "C:\\filesToDsp\\biarcs.30-of-99.txt";    //args[0];
        String inputFile = args[1];
         // String outputFile = args[1];

        // First Stage :  1 Job , Filter bicrs

        //update the details of first job
        try {
         //   System.setProperty("hadoop.home.dir", "C:\\Users\\reuty\\hadoop-2.6.0\\");
            JobControl filterJC = new JobControl("filter input");
            Configuration FilterConf = new Configuration();
            Job FilterJob = Job.getInstance(FilterConf, "FilterJob");
            FilterJob.setJarByClass(Filtered.class);
            //FilterJob.setMapperClass(Filtered.MapperClass.class);
            MultipleInputs.addInputPath(FilterJob,new Path("s3://hannadirtproject/projectinput/DIRTinput1"), TextInputFormat.class, Filtered.MapperClass.class);

            if(args.length > 0 && args[1].equals("big")) {
                MultipleInputs.addInputPath(FilterJob, new Path("s3://hannadirtproject/projectinput/DIRTinput2"), TextInputFormat.class, Filtered.MapperClass.class);
                MultipleInputs.addInputPath(FilterJob, new Path("s3://hannadirtproject/projectinput/DIRTinput3"), TextInputFormat.class, Filtered.MapperClass.class);
                MultipleInputs.addInputPath(FilterJob, new Path("s3://hannadirtproject/projectinput/DIRTinput4"), TextInputFormat.class, Filtered.MapperClass.class);
                MultipleInputs.addInputPath(FilterJob, new Path("s3://hannadirtproject/projectinput/DIRTinput5"), TextInputFormat.class, Filtered.MapperClass.class);
                MultipleInputs.addInputPath(FilterJob, new Path("s3://hannadirtproject/projectinput/DIRTinput6"), TextInputFormat.class, Filtered.MapperClass.class);
            }
            FilterJob.setPartitionerClass(Filtered.PartitionerClass.class);
            FilterJob.setCombinerClass(Filtered.ReducerClass.class);
            FilterJob.setReducerClass(Filtered.ReducerClass.class);
            FilterJob.setMapOutputKeyClass(Text.class);
            FilterJob.setMapOutputValueClass(Text.class);
            FilterJob.setOutputKeyClass(Text.class);
            FilterJob.setOutputValueClass(Text.class);
            //FilterJob.setInputFormatClass(SequenceFileInputFormat.class);
            FilterJob.setInputFormatClass(TextInputFormat.class);
            FilterJob.setOutputFormatClass(TextOutputFormat.class);
            ControlledJob cjFilter = new ControlledJob(FilterJob, new LinkedList<ControlledJob>());
            filterJC.addJob(cjFilter);
            //update the input File of job
            FileInputFormat.addInputPath(FilterJob, new Path(inputFile));
            FileInputFormat.addInputPath(FilterJob, new Path(args[1]));
            String JobFilterdPath = "s3://ass3bucket4/"+ FilterJob.getJobName();
            FileOutputFormat.setOutputPath(FilterJob, new Path(args[2]));
            FileOutputFormat.setOutputPath(FilterJob, new Path(JobFilterdPath));
            Thread filterThread = new Thread(filterJC);
            filterThread.start();
            while(!filterJC.allFinished()){
                System.out.println("not finished yet - filter input");
                Thread.sleep(5000L);
            }
            System.out.println("after while");
            if (filterJC.getFailedJobList().isEmpty()){
                System.out.println("failed list is empty");
            }
            else{
                for(ControlledJob cj : filterJC.getFailedJobList())
                    System.out.println(cj.getMessage());
            }
          //  filterJC.stop();

            //save the first path for delete the path later
            pathsOfOutput.add(JobFilterdPath);
            FilterJob.waitForCompletion(true);
            if (FilterJob.isSuccessful()) {
                System.out.println("Finish the first job");
            } else {
                throw new RuntimeException("Job failed : " + FilterJob);
            }
           //creating job control::

           // firstStep.stop();
         //   System.exit(0);

            /// second step::
            //Job 1:
           JobControl jobControlSumInput = new JobControl("count sums");

            Configuration confPathSums = new Configuration();
            confPathSums.setInt("index", 0);
            Job pathSumsJob = Job.getInstance(confPathSums, "pathSums");
            ControlledJob firstJob = new ControlledJob(pathSumsJob, new LinkedList<ControlledJob>());
            jobControlSumInput.addJob(firstJob);
            pathSumsJob.setJarByClass(PathAndSlotsSums.class);
            pathSumsJob.setMapperClass(PathAndSlotsSums.MapperClass.class);
            pathSumsJob.setPartitionerClass(PathAndSlotsSums.PartitionerClass.class);
            pathSumsJob.setCombinerClass(PathAndSlotsSums.ReducerClass.class);
            pathSumsJob.setReducerClass(PathAndSlotsSums.ReducerClass.class);
            pathSumsJob.setMapOutputKeyClass(Text.class);
            pathSumsJob.setMapOutputValueClass(Text.class);
            pathSumsJob.setOutputKeyClass(Text.class);
            pathSumsJob.setOutputValueClass(Text.class);
            pathSumsJob.setInputFormatClass(TextInputFormat.class);
            pathSumsJob.setOutputFormatClass(TextOutputFormat.class);

            //update the input File of job
            FileInputFormat.addInputPath(pathSumsJob, new Path(JobFilterdPath));
            String pathSumsJobPath = "s3://ass3bucket4/"+ pathSumsJob.getJobName();
            FileOutputFormat.setOutputPath(pathSumsJob, new Path(pathSumsJobPath));

            //job2:
            Configuration confsumSlot = new Configuration();
            confsumSlot.setInt("index", 1);
            Job SumSlotJob = Job.getInstance(confsumSlot, "slotsSums");
            ControlledJob secondJob = new ControlledJob(SumSlotJob, new LinkedList<ControlledJob>());
            jobControlSumInput.addJob(secondJob);
            SumSlotJob.setJarByClass(PathAndSlotsSums.class);
            SumSlotJob.setMapperClass(PathAndSlotsSums.MapperClass.class);
            SumSlotJob.setPartitionerClass(PathAndSlotsSums.PartitionerClass.class);
            SumSlotJob.setCombinerClass(PathAndSlotsSums.ReducerClass.class);
            SumSlotJob.setReducerClass(PathAndSlotsSums.ReducerClass.class);
            SumSlotJob.setMapOutputKeyClass(Text.class);
            SumSlotJob.setMapOutputValueClass(Text.class);
            SumSlotJob.setOutputKeyClass(Text.class);
            SumSlotJob.setOutputValueClass(Text.class);
            SumSlotJob.setInputFormatClass(TextInputFormat.class);
            SumSlotJob.setOutputFormatClass(TextOutputFormat.class);

            //update the input File of job
            FileInputFormat.addInputPath(SumSlotJob, new Path(JobFilterdPath));
            String SumSlotJobPath = "s3://ass3bucket4/"+ SumSlotJob.getJobName();
            FileOutputFormat.setOutputPath(SumSlotJob, new Path(SumSlotJobPath));

            //job 3:
            Configuration confSumsPathAndSlot = new Configuration();
            confSumsPathAndSlot.setInt("index", 2);
            Job SumPathAndSlot = Job.getInstance(confSumsPathAndSlot, "pathAndSlotSums");
            ControlledJob thirdJob = new ControlledJob(SumPathAndSlot, new LinkedList<ControlledJob>());
            jobControlSumInput.addJob(thirdJob);
            SumPathAndSlot.setJarByClass(PathAndSlotsSums.class);
            SumPathAndSlot.setMapperClass(PathAndSlotsSums.MapperClass.class);
            SumPathAndSlot.setPartitionerClass(PathAndSlotsSums.PartitionerClass.class);
            SumPathAndSlot.setCombinerClass(PathAndSlotsSums.ReducerClass.class);
            SumPathAndSlot.setReducerClass(PathAndSlotsSums.ReducerClass.class);
            SumPathAndSlot.setMapOutputKeyClass(Text.class);
            SumPathAndSlot.setMapOutputValueClass(Text.class);
            SumPathAndSlot.setOutputKeyClass(Text.class);
            SumPathAndSlot.setOutputValueClass(Text.class);
            SumPathAndSlot.setInputFormatClass(TextInputFormat.class);
            SumPathAndSlot.setOutputFormatClass(TextOutputFormat.class);

            //update the input File of job
            FileInputFormat.addInputPath(SumPathAndSlot, new Path(JobFilterdPath));
            String SumPathAndSlotPath = "s3://ass3bucket4/"+ SumPathAndSlot.getJobName();
            FileOutputFormat.setOutputPath(SumPathAndSlot, new Path(SumPathAndSlotPath));


            ///// accting The thread of the job control
            Thread countSumsThread = new Thread(jobControlSumInput);
            countSumsThread.start();
            while(!jobControlSumInput.allFinished()){
                System.out.println("not finished yet - count sums");
                Thread.sleep(5000L);
            }
            jobControlSumInput.stop();

            //Stage 3
            JobControl calcMi = new JobControl("calc mi");
            //job 4
            long N =-1;
            while(N == -1){
              Counters cs = FilterJob.getCounters();
              Counter c = cs.findCounter(Filtered.ReducerClass.Counter.N_COUNTER);
              N = c.getValue();
            }
            Configuration confJoinPathAndPathSlot = new Configuration();
            confJoinPathAndPathSlot.setLong("N", N);
            Job joinPathAnsPathSlotJob = Job.getInstance(confJoinPathAndPathSlot, "JoinPathAndPAthSlot");
            joinPathAnsPathSlotJob.setJarByClass(JoinPathAndPathSlot.class);
            joinPathAnsPathSlotJob.setPartitionerClass(JoinPathAndPathSlot.PartitionerClass.class);
            joinPathAnsPathSlotJob.setReducerClass(JoinPathAndPathSlot.ReducerClass.class);
            joinPathAnsPathSlotJob.setMapOutputKeyClass(Text.class);
            joinPathAnsPathSlotJob.setMapOutputValueClass(Text.class);
            joinPathAnsPathSlotJob.setOutputKeyClass(Text.class);
            joinPathAnsPathSlotJob.setOutputValueClass(Text.class);
            MultipleInputs.addInputPath(joinPathAnsPathSlotJob, new Path(pathSumsJobPath),
                    TextInputFormat.class, JoinPathAndPathSlot.MapperClassPath.class);
            MultipleInputs.addInputPath(joinPathAnsPathSlotJob, new Path (SumPathAndSlotPath),
                    TextInputFormat.class, JoinPathAndPathSlot.MapperClassPathSlot.class);
            //joinPathAnsPathSlotJob.setInputFormatClass(TextInputFormat.class);
            joinPathAnsPathSlotJob.setOutputFormatClass(TextOutputFormat.class);

            //update the input File of job
             FileInputFormat.addInputPath(joinPathAnsPathSlotJob, new Path(JobFilterdPath));
            String JoinPathAndPathSlotPath = "s3://ass3bucket4/"+ joinPathAnsPathSlotJob.getJobName();
            FileOutputFormat.setOutputPath(joinPathAnsPathSlotJob, new Path(JoinPathAndPathSlotPath));
            ControlledJob fourJob = new ControlledJob(joinPathAnsPathSlotJob, new LinkedList<ControlledJob>());
            fourJob.addDependingJob(firstJob);
            fourJob.addDependingJob(thirdJob);
            calcMi.addJob(fourJob);

            //job 5
            Configuration confJoinSlotAndR = new Configuration();
            Job JoinSlotAndRJob = Job.getInstance(confJoinSlotAndR, "JoinSlotAndR");

            JoinSlotAndRJob.setJarByClass(JoinSLotAndR.class);
            JoinSlotAndRJob.setPartitionerClass(JoinSLotAndR.PartitionerClass.class);
            MultipleInputs.addInputPath(JoinSlotAndRJob, new Path(SumSlotJobPath),
                    TextInputFormat.class, JoinSLotAndR.MapperClassSLots.class);
            MultipleInputs.addInputPath(JoinSlotAndRJob, new Path (JoinPathAndPathSlotPath),
                    TextInputFormat.class, JoinSLotAndR.MapperClassResultFromJoinPath.class);
            //  joinPathAnsPathSlotJob.setCombinerClass(JoinPathAndPathSlot.ReducerClass.class);
            JoinSlotAndRJob.setReducerClass(JoinSLotAndR.ReducerClass.class);
            JoinSlotAndRJob.setMapOutputKeyClass(Text.class); //?
            JoinSlotAndRJob.setMapOutputValueClass(Text.class);  //?
            JoinSlotAndRJob.setOutputKeyClass(Text.class);
            JoinSlotAndRJob.setOutputValueClass(Text.class);
            JoinSlotAndRJob.setInputFormatClass(TextInputFormat.class);
            JoinSlotAndRJob.setOutputFormatClass(TextOutputFormat.class);

            //update the input File of job
            //  FileInputFormat.addInputPath(JoinSlotAndRJob, new Path(JobFilterdPath));
            String JoinSlotAndRPath = "s3://ass3bucket1/"+ JoinSlotAndRJob.getJobName();
            FileOutputFormat.setOutputPath(JoinSlotAndRJob, new Path(JoinSlotAndRPath));

            Configuration calcMiSlotsConf = new Configuration();
            Job calcMiSlotsJob = Job.getInstance(calcMiSlotsConf, "calcMiSlots");
            calcMiSlotsJob.setJarByClass(MiFile.class);
            calcMiSlotsJob.setReducerClass(MiFile.ReducerClass.class);
            calcMiSlotsJob.setPartitionerClass(MiFile.PartitionerClass.class);
            calcMiSlotsJob.setOutputKeyClass(Text.class);
            calcMiSlotsJob.setOutputValueClass(Text.class);
            MultipleInputs.addInputPath(calcMiSlotsJob, new Path(SumSlotJobPath),
                    TextInputFormat.class, MiFile.MapperClassSlotSums.class);
            MultipleInputs.addInputPath(calcMiSlotsJob, new Path (JoinPathAndPathSlotPath),
                    TextInputFormat.class, MiFile.MapperClassJoinedPath.class);
            String path ="s3://ass3bucket4/" + "/Mi";
            FileOutputFormat.setOutputPath(calcMiSlotsJob, new Path (path));
            ControlledJob cjCalcMiSlots = new ControlledJob(calcMiSlotsJob, new LinkedList<ControlledJob>());
            cjCalcMiSlots.addDependingJob(fourJob);
            calcMi.addJob(cjCalcMiSlots);

            Thread calcMiThread = new Thread(calcMi);
            calcMiThread.start();
            while(!calcMi.allFinished()){
                System.out.println("not finished yet - calc mi");
                Thread.sleep(5000L);
            }
            calcMi.stop();
            filterJC.stop();

            calcMiSlotsJob.waitForCompletion(true);
            if (calcMiSlotsJob.isSuccessful()) {
                System.out.println("Finish the first job");
            } else {
                throw new RuntimeException("Job failed : " + calcMiSlotsJob);
            }
            System.exit(0);
        } catch (IOException | InterruptedException e) {
            e.printStackTrace();
        }
    }



}
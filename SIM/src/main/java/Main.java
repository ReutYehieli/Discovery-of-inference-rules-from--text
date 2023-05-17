
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


import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.LinkedList;
import software.amazon.awssdk.core.ResponseBytes;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.GetObjectResponse;
public class Main {
    public static void main(String[] args) throws IOException, InterruptedException {
        String inputFile = "s3://ass3bucket1/JoinSlotAndR";
        String output = args[2];
        try {
            // getting the test file into string
            S3Client s3 = S3Client.builder().region(Region.US_EAST_1).build();
            software.amazon.awssdk.services.s3.model.GetObjectRequest request = software.amazon.awssdk.services.s3.model.GetObjectRequest.builder().key(args[1]).bucket("ass3bucket1").build();
            ResponseBytes<GetObjectResponse> responseBytes = s3.getObjectAsBytes(request);                                                           //needtobe here the name ofthe file
            byte[] data = responseBytes.asByteArray();
            String testFile = new String(data, StandardCharsets.UTF_8);
            //firstJob
            Configuration FilterMiConf = new Configuration();
            FilterMiConf.set("testPathString",testFile);
            Job Job1 = Job.getInstance(FilterMiConf, "FilterMi");
            Job1.setJarByClass(FilterMI.class);
            Job1.setMapperClass(FilterMI.MapperClass.class);
            Job1.setPartitionerClass(FilterMI.PartitionerClass.class);
            //Job1.setCombinerClass(FilterMI.ReducerClass.class);
            Job1.setReducerClass(FilterMI.ReducerClass.class);
            Job1.setMapOutputKeyClass(Text.class);
            Job1.setMapOutputValueClass(Text.class);
            Job1.setOutputKeyClass(Text.class);
            Job1.setOutputValueClass(Text.class);
            // Job1.setInputFormatClass(SequenceFileInputFormat.class);
            Job1.setInputFormatClass(TextInputFormat.class);
            Job1.setOutputFormatClass(TextOutputFormat.class);

            //update the input File of job
            FileInputFormat.addInputPath(Job1, new Path(inputFile));
            //FileInputFormat.addInputPath(FilterJob, new Path(args[1]));
            String JobFilterdPath = "s3://ass3bucket3/" + Job1.getJobName();
            //  FileOutputFormat.setOutputPath(FilterJob, new Path(args[2]));
            FileOutputFormat.setOutputPath(Job1, new Path(JobFilterdPath));

            JobControl firstStep = new JobControl("filter input");
            ControlledJob filterJobC = new ControlledJob(Job1, new LinkedList<ControlledJob>());
            firstStep.addJob(filterJobC);

            // creating the Thread of firstStep and runing it
            Thread firstThread = new Thread(firstStep);
            firstThread.start();
            while (!firstStep.allFinished()) {
                System.out.println("not finished yet - filter input");
                Thread.sleep(5000L);
            }
            System.out.println("after while");
            if (firstStep.getFailedJobList().isEmpty()) {
                System.out.println("failed list is empty");
            } else {
                for (ControlledJob cj : firstStep.getFailedJobList())
                    System.out.println(cj.getMessage());
            }
///////////////////////// stage 2/////////////////////////////////
            JobControl amountTheDetails = new JobControl("amountTheDetails");

             //Job of creating the table///
            Configuration conf3 = new Configuration();
            Job job3 = Job.getInstance(conf3, "CountForWordAllPaths");
            job3.setJarByClass(CountForWordAllPaths.class);
            job3.setMapperClass(CountForWordAllPaths.MapperClass.class);
            job3.setPartitionerClass(CountForWordAllPaths.PartitionerClass.class);
            //Job1.setCombinerClass(FilterMI.ReducerClass.class);
            job3.setReducerClass(CountForWordAllPaths.ReducerClass.class);
            job3.setMapOutputKeyClass(Text.class);
            job3.setMapOutputValueClass(Text.class);
            job3.setOutputKeyClass(Text.class);
            job3.setOutputValueClass(Text.class);
            // Job1.setInputFormatClass(SequenceFileInputFormat.class);
            job3.setInputFormatClass(TextInputFormat.class);
            job3.setOutputFormatClass(TextOutputFormat.class);

            //update the input File of job
            FileInputFormat.addInputPath(job3, new Path(JobFilterdPath));
            //FileInputFormat.addInputPath(FilterJob, new Path(args[1]));
            String job2Path = "s3://ass3bucket3/" + job3.getJobName();
            //  FileOutputFormat.setOutputPath(FilterJob, new Path(args[2]));
            FileOutputFormat.setOutputPath(job3, new Path(job2Path));

            ControlledJob jobCjob3 = new ControlledJob(job3, new LinkedList<ControlledJob>());
            amountTheDetails.addJob(jobCjob3);


            //Job of creating the den and num in the formula///
            Configuration confCountDen = new Configuration();
            Job jobCountDen = Job.getInstance(confCountDen, "CountDen");
            jobCountDen.setJarByClass(DatailsDen.class);
            jobCountDen.setMapperClass(DatailsDen.MapperClass.class);
           // MultipleInputs.addInputPath(jobCountDen, new Path(JobFilterdPath),
                //    TextInputFormat.class, DatailsDenAndNum.MapperClassForDen.class);
            jobCountDen.setPartitionerClass(DatailsDen.PartitionerClass.class);
            //Job1.setCombinerClass(FilterMI.ReducerClass.class);
            jobCountDen.setReducerClass(DatailsDen.ReducerClass.class);
            jobCountDen.setMapOutputKeyClass(Text.class);
            jobCountDen.setMapOutputValueClass(Text.class);
            jobCountDen.setOutputKeyClass(Text.class);
            jobCountDen.setOutputValueClass(Text.class);
            jobCountDen.setNumReduceTasks(1);
            // Job1.setInputFormatClass(SequenceFileInputFormat.class);
            jobCountDen.setInputFormatClass(TextInputFormat.class);
            jobCountDen.setOutputFormatClass(TextOutputFormat.class);

            //update the input File of job
            FileInputFormat.addInputPath(jobCountDen, new Path(JobFilterdPath));
            String jobCountDenPath = "s3://ass3bucket3/" + jobCountDen.getJobName();
            FileOutputFormat.setOutputPath(jobCountDen, new Path(jobCountDenPath));

            ControlledJob jobCjobCountDen = new ControlledJob(jobCountDen, new LinkedList<ControlledJob>());
            amountTheDetails.addJob(jobCjobCountDen);

            ///for DetailsNums
            Configuration confCountNums = new Configuration();
            Job jobCountNums = Job.getInstance(confCountNums, "CountNums");
            confCountNums.setStrings("testPathString",testFile);
            jobCountNums.setJarByClass(DetailsNum.class);
            jobCountNums.setMapperClass(DetailsNum.MapperClassForNum.class);
            // MultipleInputs.addInputPath(jobCountDen, new Path(JobFilterdPath),
            //    TextInputFormat.class, DatailsDenAndNum.MapperClassForDen.class);
            jobCountNums.setPartitionerClass(DetailsNum.PartitionerClass.class);
            //Job1.setCombinerClass(FilterMI.ReducerClass.class);
            jobCountNums.setReducerClass(DetailsNum.ReducerClass.class);
            jobCountNums.setMapOutputKeyClass(Text.class);
            jobCountNums.setMapOutputValueClass(Text.class);
            jobCountNums.setOutputKeyClass(Text.class);
            jobCountNums.setOutputValueClass(Text.class);
            jobCountNums.setNumReduceTasks(1);
            // Job1.setInputFormatClass(SequenceFileInputFormat.class);
            jobCountNums.setInputFormatClass(TextInputFormat.class);
            jobCountNums.setOutputFormatClass(TextOutputFormat.class);

            //update the input File of job
            FileInputFormat.addInputPath(jobCountNums, new Path(job2Path));
            String jobCountNumsPath = "s3://ass3bucket3/" + jobCountNums.getJobName();
            FileOutputFormat.setOutputPath(jobCountNums, new Path(jobCountNumsPath));

            ControlledJob jobCjobCountNums = new ControlledJob(jobCountNums, new LinkedList<ControlledJob>());
            amountTheDetails.addJob(jobCjobCountNums);

            Thread amountTheDetailsThread = new Thread(amountTheDetails);
            amountTheDetailsThread.start();
            while (!amountTheDetails.allFinished()){
                System.out.println("not finished yet - intermediate Calc Sim");
                Thread.sleep(10000);
            }
            if (!amountTheDetails.getFailedJobList().isEmpty()){
                for (ControlledJob harta : amountTheDetails.getFailedJobList())
                    System.out.println(harta.getMessage());
            }
            amountTheDetails.stop();

            JobControl calcSimJC = new JobControl("calc sim");
            Configuration simSlotsConf = new Configuration();
            simSlotsConf.setStrings("testPathString",testFile);
            Job simSlotsJob = Job.getInstance(simSlotsConf, "simSlots");
            ControlledJob cjSimSlots = new ControlledJob(simSlotsJob, new LinkedList<ControlledJob>());
            simSlotsJob.setJarByClass(SimFile.class);
            simSlotsJob.setReducerClass(SimFile.ReducerClass.class);
            simSlotsJob.setPartitionerClass(SimFile.PartitionerClass.class);
            simSlotsJob.setOutputKeyClass(Text.class);
            simSlotsJob.setOutputValueClass(Text.class);
            MultipleInputs.addInputPath(simSlotsJob, new Path(jobCountDenPath),
                    TextInputFormat.class, SimFile.MapperClassPaths.class);
            MultipleInputs.addInputPath(simSlotsJob, new Path (jobCountNumsPath),
                    TextInputFormat.class, SimFile.MapperClassPairs.class);
            String pathSimSlots = "s3://ass3bucket3"+simSlotsJob.getJobName();
            simSlotsJob.setNumReduceTasks(1);
            FileOutputFormat.setOutputPath(simSlotsJob, new Path(pathSimSlots));
            calcSimJC.addJob(cjSimSlots);

            Thread calcSimThread = new Thread(calcSimJC);
            calcSimThread.start();
            while (!calcSimJC.allFinished()){
                System.out.println("not finished yet - Calc Sim");
                Thread.sleep(10000);
            }
            if (!calcSimJC.getFailedJobList().isEmpty()){
                for (ControlledJob harta : calcSimJC.getFailedJobList())
                    System.out.println(harta.getMessage());
            }
            calcSimJC.stop();

            JobControl pathSimSortJc = new JobControl("paths sim and sort");
            Configuration pathsSimConf = new Configuration();
            Job pathsSimJob = Job.getInstance(pathsSimConf, "pathsSim");
            ControlledJob cjPathsSim = new ControlledJob(pathsSimJob, new LinkedList<ControlledJob>());
            pathsSimJob.setJarByClass(pathsSimFile.class);
            pathsSimJob.setMapperClass(pathsSimFile.MapperClass.class);
            pathsSimJob.setPartitionerClass(pathsSimFile.PartitionerClass.class);
            //job.setCombinerClass(pathsSimFile.ReducerClass.class);
            pathsSimJob.setReducerClass(pathsSimFile.ReducerClass.class);
            pathsSimJob.setMapOutputKeyClass(Text.class);
            pathsSimJob.setMapOutputValueClass(Text.class);
            pathsSimJob.setOutputKeyClass(Text.class);
            pathsSimJob.setOutputValueClass(Text.class);
            pathsSimJob.setInputFormatClass(TextInputFormat.class);
            FileInputFormat.addInputPath(pathsSimJob, new Path(pathSimSlots));
            String pathsSimpath = "s3://ass3bucket3"+pathsSimJob.getJobName();
            pathsSimJob.setNumReduceTasks(1);
            FileOutputFormat.setOutputPath(pathsSimJob, new Path (pathsSimpath));
            pathSimSortJc.addJob(cjPathsSim);


            Configuration sortConf = new Configuration();
            Job sortJob = Job.getInstance(sortConf, "sort");
            ControlledJob cjSort = new ControlledJob(sortJob, new LinkedList<ControlledJob>());
            sortJob.setJarByClass(SortFile.class);
            sortJob.setMapperClass(SortFile.MapperClass.class);
            sortJob.setPartitionerClass(SortFile.PartitionerClass.class);
            sortJob.setSortComparatorClass(SimSortComparator.class);
            //job.setCombinerClass(pathsSimFile.ReducerClass.class);
            sortJob.setReducerClass(SortFile.ReducerClass.class);
            sortJob.setMapOutputKeyClass(SimComprable.class);
            sortJob.setMapOutputValueClass(Text.class);
            sortJob.setOutputKeyClass(SimComprable.class);
            sortJob.setOutputValueClass(Text.class);
            sortJob.setInputFormatClass(TextInputFormat.class);
            sortJob.setNumReduceTasks(1);
            FileInputFormat.addInputPath(sortJob, new Path(pathsSimpath));
            String path = output + "/Similarity";
            FileOutputFormat.setOutputPath(sortJob, new Path (path));
            cjSort.addDependingJob(cjPathsSim);
            pathSimSortJc.addJob(cjSort);


        } catch (IOException | InterruptedException e) {
            e.printStackTrace();
        }
    }
}

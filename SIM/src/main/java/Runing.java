import com.amazonaws.auth.AWSCredentialsProvider;
import com.amazonaws.auth.EnvironmentVariableCredentialsProvider;
import com.amazonaws.services.ec2.model.InstanceType;
import com.amazonaws.services.elasticmapreduce.AmazonElasticMapReduce;
import com.amazonaws.services.elasticmapreduce.AmazonElasticMapReduceClientBuilder;
import com.amazonaws.services.elasticmapreduce.model.*;

import java.io.IOException;

public class Runing {
    public static void main(String[] args) throws IOException {
        AWSCredentialsProvider credentials = new EnvironmentVariableCredentialsProvider();
        AmazonElasticMapReduce mapReduce = AmazonElasticMapReduceClientBuilder.standard()
                .withCredentials(credentials)
                .withRegion("us-east-1")
                .build();

        HadoopJarStepConfig step1_config = new HadoopJarStepConfig()
                .withJar("s3://ass3bucket2/SIM.jar")
                .withMainClass("Main")
                .withArgs("s3://ass3bucket2", "s3://ass3bucket4/Mi","s3://ass3bucket4/test","s3://ass3bucket3");
                                                //input       //testinput // output
        StepConfig step1 = new StepConfig()
                .withName("Step1")
                .withHadoopJarStep(step1_config)
                .withActionOnFailure("TERMINATE_JOB_FLOW");

        JobFlowInstancesConfig instances = new JobFlowInstancesConfig()
                .withInstanceCount(8)
                .withMasterInstanceType(InstanceType.M1Large.toString())
                .withSlaveInstanceType(InstanceType.M1Large.toString())
                .withHadoopVersion("2.6.0").withEc2KeyName("vockey")
                .withKeepJobFlowAliveWhenNoSteps(false)
                .withPlacement(new PlacementType("us-east-1a"));

        RunJobFlowRequest runFlowRequest = new RunJobFlowRequest()
                .withName("ASS2-with - stackoverflow- with stop word condition")
                .withInstances(instances)
                .withSteps(step1)
                .withLogUri("s3://ass3bucket2/logs/")
                .withServiceRole("EMR_DefaultRole")
                .withJobFlowRole("EMR_EC2_DefaultRole")
                .withReleaseLabel("emr-5.11.0");

        RunJobFlowResult runJobFlowResult = mapReduce.runJobFlow(runFlowRequest);
        String jobFlowId = runJobFlowResult.getJobFlowId();

        System.out.println("Ran job flow with id: " + jobFlowId);

    }
}
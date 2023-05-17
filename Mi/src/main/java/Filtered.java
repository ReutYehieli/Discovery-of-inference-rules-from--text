import org.apache.hadoop.io.LongWritable;
import org.apache.hadoop.io.Text;
import org.apache.hadoop.mapreduce.Mapper;
import org.apache.hadoop.mapreduce.Partitioner;
import org.apache.hadoop.mapreduce.Reducer;
import tartarus.snowball2.ext.englishStemmer;


import java.io.IOException;
import java.util.Arrays;

public class Filtered {
    public static class MapperClass extends Mapper<LongWritable, Text, Text, Text> {
        private String auxVerbs;
        Integer fisrNounIndex;
        Integer secondNounIndex ;

      private String[] getPostTags(String[] biarc){
          String [] output = new String[biarc.length];
          for (int i = 0; i < biarc.length; i++){
              output[i] = biarc[i].split("/")[1];
          }
          return output;
      }
      private boolean startsWithLetter (String str){
            String tmp = str.toLowerCase();
            char c = tmp.charAt(0);
            return (c >='a' && c <= 'z');
      }

      private boolean checkVerb(String[] biarc , String head){
          for(int i = 0; i< biarc.length; i++){
              String pos = biarc[i].split("/")[1];   // The pos- tag
              if(pos.startsWith("VB")){
                  String word =biarc[i].split("/")[0];
                  if(word.equals(head)){ return true;}   // we want to make sure that the verb between the 2 words is the head!
              }
          }
          return false;
      }

      private int CheckingCondition (String[] biarc, String head){

              String[] postTags = getPostTags(biarc);
              int start = 0;
              int end = postTags.length - 1;
              boolean headIsVerb = false;
              for (int i = 1; i < postTags.length - 1 & !headIsVerb; i++) {  // i know that the head can ce the first word and the last word
                  // looking the head in ngram
                  if (biarc[i].split("/")[3].equals("0")) {
                      headIsVerb = postTags[i].startsWith("VB") && biarc[i].split("/")[0].equals(head);
                  }
              }
              if (!headIsVerb) {
                  return -1;
              }
              if (!startsWithLetter(head) || auxVerbs.contains(head)) {
                  return -1;
              }

              int numberOfWord = postTags.length;
              if (numberOfWord < 3) {
                  return -1;
              } else if (numberOfWord == 3) {
                  if (!((postTags[0].startsWith("NN") | postTags[0].startsWith("PRP")) &&
                          (postTags[2].startsWith("NN") | postTags[2].startsWith("PRP")) && startsWithLetter(biarc[0].split("/")[0]) && startsWithLetter(biarc[2].split("/")[0]) && postTags[1].startsWith("VB"))) {
                      return -1;
                  }
              } else {
                  boolean found = false;
                  while (start + 1 < end && !found) {
                      found = true;
                      if (!postTags[start].startsWith("NN") && !postTags[start].startsWith("PRP")) {
                          start = start + 1;
                          found = false;
                      }
                      if (!postTags[end].startsWith("NN") && !postTags[end].startsWith("PRP")) {
                          end = end - 1;
                          found = false;
                      }
                  }
                  if (!found || !startsWithLetter(biarc[start].split("/")[0]) || !startsWithLetter(biarc[end].split("/")[0]) || !checkVerb(Arrays.copyOfRange(biarc, start + 1, end), head)) {
                      return -1;
                  }
              }
              fisrNounIndex = start;
              secondNounIndex = end;
              String firstNounDep = biarc[start].split("/")[2];
              String secondNounDep = biarc[end].split("/")[2];
              if (firstNounDep.contains("obj") && secondNounDep.contains("subj")) return 1;
              else if (firstNounDep.contains("subj") && secondNounDep.contains("obj")) return 2;
              else return -1;

      }

        private String getPathBetweenTwoNouns(int firstIndex, int secondIndex ,String[] biarc , String verb){

          if(biarc.length == 3) return (verb);
          String[] path = Arrays.copyOfRange(biarc, firstIndex+1 , secondIndex);
          String pathAns = "";
          for( int i=0; i< path.length; i++){
              pathAns= pathAns+ path[i].split("/")[0]+" ";
          }
          // we dont want to add the last " "  in the string
            pathAns = pathAns.substring(0, pathAns.length()-1);
          return pathAns;
        }

        private String[] doStremmerOnArrayString(String[] array){
            String []output = new String[3];
            for(int i = 0 ; i<3; i++){
                String str = array[i];
                String[] strSplit = str.split(" ");
                String curr = "";
                englishStemmer s = new englishStemmer();
                for(String w : strSplit){
                    s.setCurrent(w);
                    s.stem();
                    curr += " "+s.getCurrent();
                }
                output[i] = curr.substring(1);

            }
            return output;
        }

        private int count = 0 ;
        public void setup(Context context)  throws IOException, InterruptedException {
            this.auxVerbs = "am is are were be been have has had do does did";
            fisrNounIndex = -1;
            secondNounIndex = -1;

        }


        public void map(LongWritable lineId, Text line, Context context) throws IOException,  InterruptedException {
                //syntactic-ngram:
                try {
                    String[] inputLine = line.toString().split("\t");
                    if(inputLine.length >= 4 ) {
                        String[] biarc = inputLine[1].split(" ");
                        int checking = CheckingCondition(biarc, inputLine[0]);
                        if (checking != -1) {
                            System.out.println("GETIING INTO CONTEXT    " + line.toString());
                            System.out.println("the first index is:  " + fisrNounIndex + "   The secons index:   " + secondNounIndex + "\n" + "The biarc that we are working on:  " + Arrays.toString(biarc));
                            // want the 2 nouns
                            String firstNoun = biarc[fisrNounIndex].split("/")[0];
                            String secondNoun = biarc[secondNounIndex].split("/")[0];
                            //want the path between 2 nouns.
                            String path = getPathBetweenTwoNouns(fisrNounIndex, secondNounIndex, biarc, inputLine[0]);
                            String[] stringToStremmer = new String[]{firstNoun, path, secondNoun};
                            String[]  result = doStremmerOnArrayString(stringToStremmer);
                            // now we have in tokenLemma Array the 2 nouns and the path between them :
                            if (checking == 1) {   // x _ y
                                count++;
                                System.out.println("The number of count is:" + count);
                                context.write(new Text(result[1] + "\t" +  result[0] + "$\t" + result[2] + "%"), new Text(inputLine[2]));
                            } else if (checking == 2) { // y _ x
                                System.out.println("The number of count is:" + count);
                                context.write(new Text(result[1] + "\t" + result[0] + "%\t" + result[2] + "$"), new Text(inputLine[2]));
                            }
                        }
                    }
                } catch (IOException | InterruptedException e) {
                    e.printStackTrace();
                }
        }


        public void cleanup(Context context)  throws IOException, InterruptedException {
        }
    }


    public static class ReducerClass extends Reducer<Text, Text,Text,Text> {
        //private Counter N;
        enum Counter{
            N_COUNTER
        }

        public void reduce(Text path, Iterable<Text> values, Context context) throws IOException, InterruptedException {
            try {
                for (Text value : values) {
                    context.write(path, value);
                    context.getCounter(Counter.N_COUNTER).increment(Long.parseLong(value.toString()));
                }
            } catch (NullPointerException |IOException |InterruptedException e) {
                System.out.println(e.getMessage());
            }
        }


        public void cleanup(Context context)  throws IOException, InterruptedException {
        }

    }
    public static class PartitionerClass extends Partitioner<Text, Text> {

        public int getPartition(Text key, Text value, int numPartitions) {
            return (key.hashCode() & Integer.MAX_VALUE) % numPartitions;
        }
    }


    }

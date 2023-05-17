import org.apache.hadoop.io.Text;
import org.apache.hadoop.io.WritableComparable;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;

public class SimComprable implements WritableComparable<SimComprable> {
    private String pair, firstPath;
    private Double sim;

    public String getFirstPath(){
        return this.firstPath;
    }

    public Double getSim (){
        return this.sim;
    }

    SimComprable(Text key) {
        String[] splitted = key.toString().split("\t");
        this.pair = splitted[0];
        this.firstPath = this.pair.split("/")[0];
        this.sim = Double.parseDouble(splitted[1]);
    }

    SimComprable(){

    }


    @Override
    public void write(DataOutput d) throws IOException {
        d.writeUTF(toString());
    }

    @Override
    public void readFields(DataInput di) throws IOException {
        System.out.println(di.toString());
        String input = di.readUTF();
        String[] inputByTab = input.split("\t");
        this.pair = inputByTab[0];
        this.firstPath = this.pair.split("/")[0];
        this.sim = Double.parseDouble(inputByTab[1]);
    }

    @Override
    public String toString() {
        return this.pair + "\t" + this.sim.toString();
    }

    @Override
    public int compareTo(SimComprable other) {
        String otherFirstPath = other.getFirstPath();
        Double otherSim = other.getSim();
        int firstPathCompare = this.firstPath.compareTo(otherFirstPath);
        if (firstPathCompare == 0){
            return otherSim.compareTo(this.sim);
        }
        return firstPathCompare;
    }


}

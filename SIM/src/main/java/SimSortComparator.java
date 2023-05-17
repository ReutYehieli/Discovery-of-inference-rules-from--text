import org.apache.hadoop.io.WritableComparable;
import org.apache.hadoop.io.WritableComparator;

public class SimSortComparator extends WritableComparator {
    public SimSortComparator() {
        super(SimComprable.class, true);
    }

    @Override
    public int compare(WritableComparable first, WritableComparable second) {
        SimComprable firstSim = (SimComprable) first;
        SimComprable secondSim = (SimComprable) second;
        return firstSim.compareTo(secondSim);
    }

}

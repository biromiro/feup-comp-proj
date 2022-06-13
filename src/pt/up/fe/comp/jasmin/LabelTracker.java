package pt.up.fe.comp.jasmin;

public class LabelTracker {
    private int comparisons;

    public LabelTracker() {
        this.comparisons = 0;
    }

    public int nextLabelNumber() {
        return comparisons++;
    }
}

package pt.up.fe.comp.jasmin;

public class StackLimit {
    private int runningTotal;
    private int maxSize;

    public StackLimit() {
        reset();
    }

    public void updateStack(int diff) {
        runningTotal += diff;
        maxSize = Math.max(runningTotal, maxSize);
    }

    public void reset() {
        runningTotal = 0;
        maxSize = 0;
    }

    public int size() {
        return maxSize;
    }
}

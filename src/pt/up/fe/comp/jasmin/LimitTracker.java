package pt.up.fe.comp.jasmin;

import java.util.HashSet;
import java.util.Set;

public class LimitTracker {
    private int runningTotal;
    private int maxSize;
    private Set<Integer> registers;

    public LimitTracker() {
        reset();
    }

    public void updateStack(int diff) {
        runningTotal += diff;
        maxSize = Math.max(runningTotal, maxSize);
    }

    public void updateRegisters(int regNum) {
        registers.add(regNum);
    }

    public void registersUntil(int size) {
        for (int i = 0; i < size; i++) {
            registers.add(i);
        }
    }

    public void reset() {
        runningTotal = 0;
        maxSize = 0;
        registers = new HashSet<>();
    }

    public int stackLimit() {
        return maxSize;
    }

    public int localsLimit() {
        return registers.size();
    }
}

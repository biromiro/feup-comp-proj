package pt.up.fe.comp.ollir.optimize;

import org.specs.comp.ollir.Method;
import org.specs.comp.ollir.Node;
import pt.up.fe.comp.jmm.ollir.OllirResult;

import java.util.ArrayList;
import java.util.Set;

public class DataFlowAnalysis {

    private final OllirResult ollirResult;
    private ArrayList<MethodDataFlowAnalysis> methodFlowList;

    public DataFlowAnalysis(OllirResult ollirResult) {
        this.ollirResult = ollirResult;
    }

    public void calcInOut() {
        System.out.println("building CFGs");
        ollirResult.getOllirClass().buildCFGs();
        System.out.println("built CFGs");
        ArrayList<Method> methods = ollirResult.getOllirClass().getMethods();
        this.methodFlowList = new ArrayList<>();

        for (Method method: methods) {
            MethodDataFlowAnalysis methodFlow = new MethodDataFlowAnalysis(method);
            methodFlow.calcInOut();
            methodFlowList.add(methodFlow);
        }
    }
}

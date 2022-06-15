package pt.up.fe.comp.ollir.optimize;

import org.specs.comp.ollir.Descriptor;
import org.specs.comp.ollir.Instruction;
import org.specs.comp.ollir.Method;
import pt.up.fe.comp.jmm.ollir.OllirResult;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

public class DataFlowAnalysis {

    private final OllirResult ollirResult;
    private ArrayList<MethodDataFlowAnalysis> methodFlowList;

    public DataFlowAnalysis(OllirResult ollirResult) {
        this.ollirResult = ollirResult;
    }

    public void calcInOut() {
        ollirResult.getOllirClass().buildCFGs();
        ArrayList<Method> methods = ollirResult.getOllirClass().getMethods();
        this.methodFlowList = new ArrayList<>();

        for (Method method: methods) {
            MethodDataFlowAnalysis methodFlow = new MethodDataFlowAnalysis(method, ollirResult);
            methodFlow.calcInOut();
            methodFlowList.add(methodFlow);
        }
    }

    public void buildInterferenceGraph() {
        for (MethodDataFlowAnalysis methodFlow: methodFlowList) {
            methodFlow.buildInterferenceGraph();
        }
    }

    public void colorGraph() {
        for (MethodDataFlowAnalysis methodFlow: methodFlowList) {
            methodFlow.buildInterferenceGraph();
            var num = ollirResult.getConfig().get("registerAllocation");

            methodFlow.colorInterferenceGraph(Integer.parseInt(num));
        }
    }

    public void allocateRegisters() {
        for (MethodDataFlowAnalysis methodFlow: methodFlowList) {
            HashMap<String, Descriptor> varTable = methodFlow.getMethod().getVarTable();
            for (RegisterNode node: methodFlow.getInterferenceGraph().getLocalVars()) {
                varTable.get(node.getName()).setVirtualReg(node.getRegister());
            }
            for (RegisterNode node: methodFlow.getInterferenceGraph().getParams()) {
                varTable.get(node.getName()).setVirtualReg(node.getRegister());
            }

            if (varTable.get("this") != null) {
                varTable.get("this").setVirtualReg(0);
            }
        }

    }

    public boolean eliminateDeadVars() {
        boolean hasDeadVars = false;
        for (MethodDataFlowAnalysis methodFlow: methodFlowList) {
            for (Instruction instruction: methodFlow.getMethod().getInstructions()) {
                instruction.show();
            }
            hasDeadVars = methodFlow.eliminateDeadVars() || hasDeadVars;
            for (Instruction instruction: methodFlow.getMethod().getInstructions()) {
                instruction.show();
            }
        }
        return hasDeadVars;
    }

}

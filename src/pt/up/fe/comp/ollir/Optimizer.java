package pt.up.fe.comp.ollir;

import pt.up.fe.comp.jmm.analysis.JmmSemanticsResult;
import pt.up.fe.comp.jmm.ollir.JmmOptimization;
import pt.up.fe.comp.jmm.ollir.OllirResult;
import pt.up.fe.comp.ollir.optimize.ConstantPropagationVisitor;
import pt.up.fe.comp.ollir.optimize.DataFlowAnalysis;

import java.util.Collections;
import java.util.HashMap;

public class Optimizer implements JmmOptimization {
    @Override
    public JmmSemanticsResult optimize(JmmSemanticsResult semanticsResult) {
        if (semanticsResult.getConfig().getOrDefault("optimize", "false").equals("false")) {
            return semanticsResult;
        }

        ConstantPropagationVisitor constantPropagationVisitor;

        do {
            constantPropagationVisitor = new ConstantPropagationVisitor();
            constantPropagationVisitor.visit(semanticsResult.getRootNode(), new HashMap<>());
        } while (constantPropagationVisitor.hasChanged());

        // simplify while
        constantPropagationVisitor = new ConstantPropagationVisitor(true);
        constantPropagationVisitor.visit(semanticsResult.getRootNode(), new HashMap<>());

        System.out.println(semanticsResult.getRootNode().toTree());

        return semanticsResult;
    }

    @Override
    public OllirResult toOllir(JmmSemanticsResult semanticsResult) {
        OllirGenerator ollirGenerator = new OllirGenerator(semanticsResult.getSymbolTable());
        ollirGenerator.visit(semanticsResult.getRootNode());
        String ollirCode = ollirGenerator.getCode();

        return new OllirResult(semanticsResult, ollirCode, Collections.emptyList());
    }

    @Override
    public OllirResult optimize(OllirResult ollirResult) {
        if (ollirResult.getConfig().getOrDefault("debug", "false").equals("true")) {
            System.out.println("OLLIR CODE:");
            System.out.println(ollirResult.getOllirCode());
        }

        DataFlowAnalysis dataFlowAnalysis = new DataFlowAnalysis(ollirResult);

        boolean optimizeFlag = ollirResult
                .getConfig()
                .getOrDefault("optimize", "false")
                .equals("true");

        boolean registerAllocationFlag = !ollirResult
                .getConfig()
                .getOrDefault("registerAllocation", "-1")
                .equals("-1");

        if (optimizeFlag) {
            do {
                dataFlowAnalysis.calcInOut();
            } while (dataFlowAnalysis.eliminateDeadVars());
        }

        if (registerAllocationFlag) {
            dataFlowAnalysis.calcInOut();
            dataFlowAnalysis.colorGraph();
            dataFlowAnalysis.allocateRegisters();
        }

        return ollirResult;
    }
}

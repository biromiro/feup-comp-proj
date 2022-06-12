package pt.up.fe.comp.ollir;

import com.javacc.parser.tree.Literal;
import org.specs.comp.ollir.*;
import pt.up.fe.comp.jmm.analysis.JmmSemanticsResult;
import pt.up.fe.comp.jmm.ollir.JmmOptimization;
import pt.up.fe.comp.jmm.ollir.OllirResult;
import pt.up.fe.comp.ollir.optimize.ConstantPropagationVisitor;
import pt.up.fe.comp.ollir.optimize.DataFlowAnalysis;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Set;

public class Optimizer implements JmmOptimization {
    @Override
    public JmmSemanticsResult optimize(JmmSemanticsResult semanticsResult) {

        String optimize = semanticsResult.getConfig().get("optimize");
        if (optimize == null || optimize.equals("false")) {
            return semanticsResult;
        }

        ConstantPropagationVisitor constantPropagationVisitor = null;

        while (constantPropagationVisitor == null ||
                constantPropagationVisitor.hasChanged()) {
            constantPropagationVisitor = new ConstantPropagationVisitor();
            constantPropagationVisitor.visit(semanticsResult.getRootNode(), new HashMap<>());
            System.out.println("After constant propagation:\n" +semanticsResult.getRootNode().toTree() + "\n\n");

        }


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
        String registerAllocation = ollirResult.getConfig().get("registerAllocation");
        if (registerAllocation == null || registerAllocation.equals("-1")) {
            return ollirResult;
        }

        DataFlowAnalysis dataFlowAnalysis = new DataFlowAnalysis(ollirResult);
        dataFlowAnalysis.calcInOut();
        dataFlowAnalysis.colorGraph();
        dataFlowAnalysis.allocateRegisters();
        return ollirResult;
    }

}

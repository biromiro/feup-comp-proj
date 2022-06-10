package pt.up.fe.comp.ollir;

import org.specs.comp.ollir.*;
import pt.up.fe.comp.jmm.analysis.JmmSemanticsResult;
import pt.up.fe.comp.jmm.ollir.JmmOptimization;
import pt.up.fe.comp.jmm.ollir.OllirResult;

import javax.lang.model.element.VariableElement;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Set;

public class Optimizer implements JmmOptimization {
    @Override
    public JmmSemanticsResult optimize(JmmSemanticsResult semanticsResult) {
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

        System.out.println("building CFGs");
        ollirResult.getOllirClass().buildCFGs();
        System.out.println("built CFGs");
        ArrayList<Method> methods = ollirResult.getOllirClass().getMethods();

        for (Method method: methods) {
            Node beginNode = method.getBeginNode();

            calcUseDefSucc(method, beginNode);

            ArrayList<Set<String>> in;
            ArrayList<Set<String>> out;
            /*for (;;) {

            }*/
        }

        return ollirResult;
    }

    private void setUse(Element dest) {
        if (dest instanceof ArrayOperand operand) {

        } else if (dest instanceof LiteralElement operand) {

        }
    }

    private void calcUseDefSucc(Method method, Node node) {

        if (node == null) return;

        if (node.getNodeType().equals(NodeType.BEGIN)) {
            calcUseDefSucc(method, node.getSucc1());
            calcUseDefSucc(method, node.getSucc2());
            return;
        }

        if (node.getNodeType().equals(NodeType.END)) {
            return;
        }

        if (node instanceof AssignInstruction instruction) {
            instruction.getDest();
            setUse(instruction.getDest());
        } else if (node instanceof UnaryOpInstruction instruction) {
            setUse(instruction.getOperand());

        } else if (node instanceof BinaryOpInstruction instruction) {
            setUse(instruction.getLeftOperand());

        } else if (node instanceof ReturnInstruction instruction) {
            setUse(instruction.getOperand());

        } else if (node instanceof CallInstruction instruction) {
            setUse(instruction.getFirstArg());

        } else if (node instanceof GotoInstruction instruction) {

        } else if (node instanceof GetFieldInstruction instruction) {

        } else if (node instanceof PutFieldInstruction instruction) {

        } else if (node instanceof SingleOpInstruction instruction) {

        } else if (node instanceof OpCondInstruction instruction) {

        } else if (node instanceof SingleOpCondInstruction instruction) {

        }

            // instruction.getInstType()

    }

}

package pt.up.fe.comp.ollir.optimize;

import pt.up.fe.comp.jmm.ast.JmmNode;
import pt.up.fe.comp.jmm.ast.JmmNodeImpl;

public class ConstantFolder {

    public static void replaceNode (JmmNode nodeToReplace, JmmNode newNode) {
        JmmNode parent = nodeToReplace.getJmmParent();
        if (parent == null) return;
        int index = parent.getChildren().indexOf(nodeToReplace);
        parent.removeJmmChild(nodeToReplace);
        parent.add(newNode, index);
        newNode.setParent(parent);
    }

    public void foldBinOpInt(JmmNode left, JmmNode right, JmmNode originalNode) {
        int leftValue = Integer.parseInt(left.get("val"));
        int rightValue = Integer.parseInt(right.get("val"));
        int result = switch (originalNode.get("op")) {
            case "+" -> leftValue + rightValue;
            case "-" -> leftValue - rightValue;
            case "*" -> leftValue * rightValue;
            case "/" -> leftValue / rightValue;
            case "<" -> leftValue < rightValue ? 1 : 0;
            case ">=" -> leftValue >= rightValue ? 1 : 0;
            default -> throw new IllegalStateException("Unexpected value: " + originalNode.get("op"));
        };
        String kind = originalNode.get("op").matches("<|>=") ? "BooleanLiteral" : "IntegerLiteral";
        String type = kind.equals("IntegerLiteral") ? "int" : "boolean";
        JmmNode newLiteral = new JmmNodeImpl(kind);
        newLiteral.put("val", Integer.toString(result));
        newLiteral.put("type", type);
        newLiteral.put("isArray", "false");
        replaceNode(originalNode, newLiteral);
    }

    public void foldBinOpBool(JmmNode left, JmmNode right, JmmNode originalNode) {
        boolean leftValue = left.get("val").equals("1");
        boolean rightValue = right.get("val").equals("1");
        boolean result = switch (originalNode.get("op")) {
            case "&&" -> leftValue && rightValue;
            case "||" -> leftValue || rightValue;
            default -> throw new IllegalStateException("Unexpected value: " + originalNode.get("op"));
        };

        JmmNode newLiteral = new JmmNodeImpl(left.getKind());
        newLiteral.put("val", result? "1" : "0");
        newLiteral.put("type", left.get("type"));
        newLiteral.put("isArray", "false");
        replaceNode(originalNode, newLiteral);
    }

    public void foldNotExpr(JmmNode expression, JmmNode jmmNode) {
        JmmNode newLiteral = new JmmNodeImpl(expression.getKind());
        newLiteral.put("val", expression.get("val").equals("1") ? "0" : "1");
        newLiteral.put("type", expression.get("type"));
        newLiteral.put("isArray", "false");
        replaceNode(jmmNode, newLiteral);
    }

    public void foldConstantIf(JmmNode expr, JmmNode jmmNode) {
        replaceNode(jmmNode, expr);
    }

    public void foldConstantWhile(JmmNode jmmNode) {
        JmmNode whileParent = jmmNode.getJmmParent();
        System.out.println(whileParent.toTree());
        whileParent.removeJmmChild(jmmNode);
    }
}

package pt.up.fe.comp.ollir.optimize;

import pt.up.fe.comp.IntegerLiteral;
import pt.up.fe.comp.jmm.ast.AJmmVisitor;
import pt.up.fe.comp.jmm.ast.JmmNode;
import pt.up.fe.comp.jmm.ast.JmmNodeImpl;
import pt.up.fe.comp.ollir.Action;

import java.util.HashMap;
import java.util.Map;

public class ConstantPropagationVisitor extends AJmmVisitor<HashMap<String, JmmNode>, String> {
    private boolean hasChanged = false;

    public ConstantPropagationVisitor(){
        addVisit("MethodBody", this::methodBodyVisit);
        addVisit("Identifier", this::identifierVisit);
        addVisit("Assignment", this::assignmentVisit);
        addVisit("IntegerLiteral", this::terminalVisit);
        addVisit("BooleanLiteral", this::terminalVisit);
        addVisit("BinaryOp", this::binaryOpVisit);
        addVisit("NotExpression", this::notExpressionVisit);
        addVisit("IfThenElseStatement", this::ifThenElseStatementVisit);
        addVisit("WhileStatement", this::whileStatementVisit);

        setDefaultVisit(this::defaultVisit);
    }

    private void fixScopedConstantsMap(HashMap<String, JmmNode> constantsMap, HashMap<String, JmmNode> newConstantsMap) {
        for (Map.Entry<String, JmmNode> entry : newConstantsMap.entrySet()) {
            JmmNode changedNode = constantsMap.get(entry.getKey());
            if (changedNode == null) continue;

            if (!changedNode.get("val").equals(entry.getValue().get("val")))
                constantsMap.remove(entry.getKey());
        }

        for (String key: constantsMap.keySet()) {
            if (newConstantsMap.get(key) == null)
                constantsMap.remove(key);
        }
    }

    private String methodBodyVisit(JmmNode jmmNode, HashMap<String, JmmNode> constantsMap) {
        HashMap<String, JmmNode> newConstantsMap = new HashMap<>(constantsMap);
        for (JmmNode child : jmmNode.getChildren()) {
            visit(child, newConstantsMap);
        }
        fixScopedConstantsMap(constantsMap, newConstantsMap);
        return "";
    }

    private String defaultVisit(JmmNode jmmNode, HashMap<String, JmmNode> constantsMap) {
        for(JmmNode child: jmmNode.getChildren()) {
            visit(child, constantsMap);
        }
        return "";
    }

    private String assignmentVisit(JmmNode jmmNode, HashMap<String, JmmNode> constantsMap) {
        JmmNode identifier = jmmNode.getJmmChild(0);
        JmmNode expression = jmmNode.getJmmChild(1);
        if (identifier.getKind().equals("Indexing")) {
            visit(identifier, constantsMap);
            return "";
        }
        if (expression.getKind().equals("IntegerLiteral") || expression.getKind().equals("BooleanLiteral")) {
            constantsMap.put(identifier.get("name"), expression);
        } else {
            visit(expression, constantsMap);
            constantsMap.remove(identifier.get("name"));
        }
        return "";
    }

    private String terminalVisit(JmmNode jmmNode, HashMap<String, JmmNode> constantsMap) {
        return "";
    }

    private String binaryOpVisit(JmmNode jmmNode, HashMap<String, JmmNode> constantsMap) {
        JmmNode left = jmmNode.getJmmChild(0);
        JmmNode right = jmmNode.getJmmChild(1);
        visit(left, constantsMap);
        visit(right, constantsMap);
        System.out.println("IM HERE");
        if (left.getKind().equals("IntegerLiteral") && right.getKind().equals("IntegerLiteral")) {
            int leftValue = Integer.parseInt(left.get("val"));
            int rightValue = Integer.parseInt(right.get("val"));
            int result = 0;
            switch (jmmNode.get("op")) {
                case "+":
                    result = leftValue + rightValue;
                    break;
                case "-":
                    result = leftValue - rightValue;
                    break;
                case "*":
                    result = leftValue * rightValue;
                    break;
                case "/":
                    result = leftValue / rightValue;
                    break;
                case "<":
                    result = leftValue < rightValue ? 1 : 0;
                    break;
            }
            String kind = jmmNode.get("op").equals("<") ? "BooleanLiteral" : "IntegerLiteral";
            String type = kind.equals("IntegerLiteral") ? "int" : "boolean";
            JmmNode newLiteral = new JmmNodeImpl(kind);
            newLiteral.put("val", Integer.toString(result));
            newLiteral.put("type", type);
            newLiteral.put("isArray", "false");
            jmmNode.replace(newLiteral);
            hasChanged = true;
            return "";
        }

        else if (left.getKind().equals("BooleanLiteral") && right.getKind().equals("BooleanLiteral")) {
            boolean leftValue = Boolean.parseBoolean(left.get("val"));
            boolean rightValue = Boolean.parseBoolean(right.get("val"));
            boolean result = false;
            if ("&&".equals(jmmNode.get("op"))) {
                result = leftValue && rightValue;
            } else return "";

            JmmNode newLiteral = new JmmNodeImpl(left.getKind());
            newLiteral.put("val", String.valueOf(result));
            newLiteral.put("type", left.get("type"));
            newLiteral.put("isArray", "false");
            jmmNode.replace(newLiteral);
            hasChanged = true;
            return "";
        } else {
            visit(left, constantsMap);
            visit(right, constantsMap);
        }

        return "";
    }

    private String identifierVisit(JmmNode jmmNode, HashMap<String, JmmNode> constantsMap) {
        JmmNode constant = constantsMap.get(jmmNode.get("name"));
        if (constant != null) {
            JmmNode newLiteral = new JmmNodeImpl(constant.getKind());
            newLiteral.put("val", constant.get("val"));
            newLiteral.put("type", constant.get("type"));
            newLiteral.put("isArray", constant.get("isArray"));
            jmmNode.replace(newLiteral);
            hasChanged = true;
        }
        return "";
    }

    private String notExpressionVisit(JmmNode jmmNode, HashMap<String, JmmNode> constantsMap) {
        JmmNode expression = jmmNode.getJmmChild(0);
        visit(expression, constantsMap);
        if (expression.getKind().equals("BooleanLiteral")) {
            JmmNode newLiteral = new JmmNodeImpl(expression.getKind());
            newLiteral.put("val", expression.get("val").equals("1") ? "0" : "1");
            newLiteral.put("type", expression.get("type"));
            newLiteral.put("isArray", "false");
            jmmNode.replace(newLiteral);
            hasChanged = true;
        }
        return "";
    }

    private String ifThenElseStatementVisit(JmmNode jmmNode, HashMap<String, JmmNode> constantsMap) {
        JmmNode condition = jmmNode.getJmmChild(0);
        JmmNode thenStatement = jmmNode.getJmmChild(1);
        JmmNode elseStatement = jmmNode.getJmmChild(2);


        visit(condition, constantsMap);

        if (condition.getKind().equals("BooleanLiteral")) {
            if (condition.get("val").equals("1")) {
                visit(thenStatement, constantsMap);
                jmmNode.replace(thenStatement);
            } else {
                visit(elseStatement, constantsMap);
                jmmNode.replace(elseStatement);
            }
            hasChanged = true;
            return "";
        }
        HashMap<String, JmmNode> newConstantsMap1 = new HashMap<>(constantsMap);

        visit(thenStatement, newConstantsMap1);

        HashMap<String, JmmNode> newConstantsMap2 = new HashMap<>(constantsMap);

        visit(elseStatement, newConstantsMap2);

        fixScopedConstantsMap(constantsMap, newConstantsMap1);
        fixScopedConstantsMap(constantsMap, newConstantsMap2);

        return "";
    }

    private String whileStatementVisit(JmmNode jmmNode, HashMap<String, JmmNode> constantsMap) {
        JmmNode condition = jmmNode.getJmmChild(0);
        JmmNode body = jmmNode.getJmmChild(1);

        visit(condition, constantsMap);

        if (condition.getKind().equals("BooleanLiteral")) {
            if (condition.get("val").equals("0")) {
                JmmNode whileParent = jmmNode.getJmmParent();
                whileParent.removeJmmChild(jmmNode);
            }
            return "";
        }
        visit(body, new HashMap<>());
        return "";
    }

    public boolean hasChanged() {
        return hasChanged;
    }
}
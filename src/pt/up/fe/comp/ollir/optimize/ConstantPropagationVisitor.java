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

    private String binaryOpVisit(JmmNode jmmNode, HashMap<String, JmmNode> constantsMap) {
        JmmNode left = jmmNode.getJmmChild(0);
        JmmNode right = jmmNode.getJmmChild(1);
        visit(left, constantsMap);
        visit(right, constantsMap);

        ConstantFolder folder = new ConstantFolder();

        if (left.getKind().equals("IntegerLiteral") && right.getKind().equals("IntegerLiteral")) {
            folder.foldBinOpInt(left, right, jmmNode);
            hasChanged = true;
        }
        else if (left.getKind().equals("BooleanLiteral") && right.getKind().equals("BooleanLiteral")) {
            folder.foldBinOpBool(left, right, jmmNode);
            hasChanged = true;
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
            ConstantFolder.replaceNode(jmmNode, newLiteral);
            hasChanged = true;
        }
        return "";
    }

    private String notExpressionVisit(JmmNode jmmNode, HashMap<String, JmmNode> constantsMap) {
        JmmNode expression = jmmNode.getJmmChild(0);
        visit(expression, constantsMap);
        ConstantFolder folder = new ConstantFolder();
        if (expression.getKind().equals("BooleanLiteral")) {
            folder.foldNotExpr(expression, jmmNode);
            hasChanged = true;
        }
        return "";
    }

    private String ifThenElseStatementVisit(JmmNode jmmNode, HashMap<String, JmmNode> constantsMap) {
        JmmNode condition = jmmNode.getJmmChild(0);
        JmmNode thenStatement = jmmNode.getJmmChild(1);
        JmmNode elseStatement = jmmNode.getJmmChild(2);


        visit(condition, constantsMap);

        ConstantFolder folder = new ConstantFolder();

        if (condition.getKind().equals("BooleanLiteral")) {
            JmmNode nextExpr = condition.get("val").equals("1") ? thenStatement : elseStatement;
            visit(nextExpr, constantsMap);
            folder.foldConstantIf(nextExpr, jmmNode);
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

        ConstantFolder folder = new ConstantFolder();

        if (condition.getKind().equals("BooleanLiteral")) {
            if (condition.get("val").equals("0")) {
                folder.foldConstantWhile(jmmNode);
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
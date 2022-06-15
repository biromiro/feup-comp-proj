package pt.up.fe.comp.ollir.optimize;

import pt.up.fe.comp.IntegerLiteral;
import pt.up.fe.comp.jmm.ast.AJmmVisitor;
import pt.up.fe.comp.jmm.ast.JmmNode;
import pt.up.fe.comp.jmm.ast.JmmNodeImpl;
import pt.up.fe.comp.ollir.Action;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class ConstantPropagationVisitor extends AJmmVisitor<HashMap<String, JmmNode>, String> {
    private boolean hasChanged = false;
    private final boolean simplifyWhile;
    public ConstantPropagationVisitor() {
        this(false);
    }
    public ConstantPropagationVisitor(boolean simplifyWhile){
        this.simplifyWhile = simplifyWhile;

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

        Set<String> keySet = new HashSet<String>(constantsMap.keySet());

        for (String key: keySet) {
            if (newConstantsMap.get(key) == null)
                constantsMap.remove(key);
        }
    }

    private String methodBodyVisit(JmmNode jmmNode, HashMap<String, JmmNode> constantsMap) {
        HashMap<String, JmmNode> newConstantsMap = new HashMap<>(constantsMap);
        for (int i=0; i<jmmNode.getChildren().size(); i++) {
            JmmNode child = jmmNode.getJmmChild(i);
            visit(child, newConstantsMap);
        }
        fixScopedConstantsMap(constantsMap, newConstantsMap);
        return "";
    }

    private String defaultVisit(JmmNode jmmNode, HashMap<String, JmmNode> constantsMap) {
        for (int i=0; i<jmmNode.getChildren().size(); i++) {
            JmmNode child = jmmNode.getJmmChild(i);
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
        String ret = visit(left, constantsMap);
        String ret2 = visit(right, constantsMap);
        ret = ret.equals("") ? ret2 : ret;
        ConstantFolder folder = new ConstantFolder();

        if (left.getKind().equals("IntegerLiteral") && right.getKind().equals("IntegerLiteral")) {
            folder.foldBinOpInt(left, right, jmmNode);
            hasChanged = true;
            return "changed";
        }
        else if (left.getKind().equals("BooleanLiteral") && right.getKind().equals("BooleanLiteral")) {
            folder.foldBinOpBool(left, right, jmmNode);
            hasChanged = true;
            return "changed";
        }

        return ret;
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
            return "changed";
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
            return "changed";
        }
        if (expression.getKind().equals("BinaryOp")) {
            String operator = expression.get("op");
            switch (operator) {
                case "<" -> expression.put("op", ">=");
                case ">=" -> expression.put("op", "<");
                default -> {
                    return binaryOpVisit(expression, constantsMap);
                }
            }
            ConstantFolder.replaceNode(jmmNode, expression);
            hasChanged = true;
            return "changed";
        }
        if (expression.getKind().equals("NotExpression")) {
            ConstantFolder.replaceNode(jmmNode, expression.getJmmChild(0));
            hasChanged = true;
            return "changed";
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

    private JmmNode copyNode(JmmNode jmmNode, JmmNode parent) {
        JmmNode copy = new JmmNodeImpl(jmmNode.getKind());

        for (String attr: jmmNode.getAttributes()) {
            copy.put(attr, jmmNode.get(attr));
        }

        for (int i=0; i < jmmNode.getChildren().size(); i++) {
            JmmNode child = jmmNode.getJmmChild(i);
            JmmNode copyChild = copyNode(child, copy);
            copy.add(copyChild, i);
            child.setParent(copy);
        }
        copy.setParent(parent);

        return copy;
    }

    private JmmNode getSimplifiedCondition(JmmNode whileStatement, HashMap<String, JmmNode> constantsMap) {
        JmmNode whileCopy = copyNode(whileStatement, whileStatement.getJmmParent());
        JmmNode condition = whileCopy.getJmmChild(0);
        String change;

        do {
            change = visit(condition, constantsMap);
            whileCopy = copyNode(whileCopy, whileStatement.getJmmParent());
            condition = whileCopy.getJmmChild(0);
        } while (change.equals("changed") && !condition.getKind().equals("BooleanLiteral"));
        return condition;
    }

    private void getScopedConstantsMap(JmmNode jmmNode, HashMap<String, JmmNode> constantsMap) {
        if (jmmNode.getKind().equals("Assignment")) {
            JmmNode identifier = jmmNode.getJmmChild(0);
            String name = identifier.get("name");
            constantsMap.remove(name);
        }

        for (JmmNode jmmNodeChild: jmmNode.getChildren()) {
            getScopedConstantsMap(jmmNodeChild, constantsMap);
        }
    }

    private String whileStatementVisit(JmmNode jmmNode, HashMap<String, JmmNode> constantsMap) {
        JmmNode condition = jmmNode.getJmmChild(0);
        JmmNode body = jmmNode.getJmmChild(1);
        HashMap<String, JmmNode> newConstantsMap = new HashMap<>();
        HashMap<String, JmmNode> constantsMapCopy = new HashMap<>(constantsMap);

        if (simplifyWhile) {
            JmmNode simplifiedCondition = getSimplifiedCondition(jmmNode, constantsMapCopy);

            ConstantFolder folder = new ConstantFolder();

            if (simplifiedCondition.getKind().equals("BooleanLiteral")) {
                if (simplifiedCondition.get("val").equals("0")) {
                    folder.foldConstantWhile(jmmNode);
                } else {
                    jmmNode.put("dowhile", "1");
                }
            }

            for (int i=0; i < condition.getChildren().size(); i++) {
                JmmNode child = condition.getJmmChild(i);
                child.setParent(condition);
            }

        } else visit(condition, newConstantsMap);

        getScopedConstantsMap(body, constantsMapCopy);
        visit(body, constantsMapCopy);
        fixScopedConstantsMap(constantsMap, constantsMapCopy);

        return "";
    }

    public boolean hasChanged() {
        return hasChanged;
    }
}
package pt.up.fe.comp;

import pt.up.fe.comp.jmm.analysis.table.Symbol;
import pt.up.fe.comp.jmm.ast.AJmmVisitor;
import pt.up.fe.comp.jmm.ast.JmmNode;
import pt.up.fe.comp.jmm.analysis.table.Type;

import java.util.ArrayList;
import java.util.List;

public class AnalysisVisitor extends AJmmVisitor<ConcreteSymbolTable, Void> {
    public AnalysisVisitor() {
        addVisit("ImportDeclaration", this::visitImport);
        addVisit("ClassDeclaration", this::visitClassDeclaration);
        addVisit("MethodDef", this::visitMethod);
        addVisit("MainMethodDef", this::visitMethod);
    }

    private Void visitImport(JmmNode jmmNode, ConcreteSymbolTable table) {
        StringBuilder importPath = new StringBuilder();
        for (JmmNode child: jmmNode.getChildren()) {
            importPath.append(child.get("path"));
            importPath.append('.');
        }

        importPath.deleteCharAt(importPath.length()-1);
        table.addImport(importPath.toString());

        return null;
    }

    private Void visitClassDeclaration(JmmNode jmmNode, ConcreteSymbolTable table) {
        table.setClassName(jmmNode.get("classname"));
        table.setSuper(jmmNode.get("extends"));

        for (JmmNode child: jmmNode.getChildren()) {
            if (child.getKind() == "VarDeclaration") {
                Symbol symbol = getSymbol(jmmNode);
                table.addField(symbol);
            }
        }

        return null;
    }

    private Void visitMethod(JmmNode jmmNode, ConcreteSymbolTable table) {
        StringBuilder methodNameBuilder = new StringBuilder();
        methodNameBuilder.append(jmmNode.get("name"));
        Type returnType = getType(jmmNode);
        List<Symbol> parameters = new ArrayList<>();
        List<Symbol> variables = new ArrayList<>();

        for (JmmNode child: jmmNode.getChildren()) {
            if (child.getKind() == "Parameter") {
                Symbol parameter = getSymbol(child);
                parameters.add(parameter);
                methodNameBuilder.append("#");
                methodNameBuilder.append(getTypeMethodNameCode(child));
            } else if (child.getKind() == "MethodBody") {
                for (JmmNode grandchild: child.getChildren()) {
                    if (grandchild.getKind() == "VarDeclaration") {
                        Symbol localVariable = getSymbol(grandchild);
                        variables.add(localVariable);
                    }
                }
            }
        }

        String methodName = methodNameBuilder.toString();

        table.addMethod(methodName, returnType, parameters, variables);
        return null;
    }

    private String getTypeMethodNameCode(JmmNode jmmNode) {
        StringBuilder builder = new StringBuilder();
        JmmNode child = jmmNode.getJmmChild(0);
        String type = child.get("type");
        boolean isArray = child.get("isArray").equals("true"));
        builder.append(type);
        if (isArray) {
            builder.append("[]");
        }
        return builder.toString();
    }

    private Type getType(JmmNode jmmNode) {
        JmmNode child = jmmNode.getJmmChild(0);
        return new Type(child.get("type"), child.get("isArray").equals("true"));
    }

    private Symbol getSymbol(JmmNode jmmNode) {
        String name = jmmNode.get("name");
        Type type = getType(jmmNode);
        return new Symbol(type, name);
    }
}

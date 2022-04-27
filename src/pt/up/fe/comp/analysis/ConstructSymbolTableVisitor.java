package pt.up.fe.comp.analysis;

import pt.up.fe.comp.jmm.analysis.table.Symbol;
import pt.up.fe.comp.jmm.ast.AJmmVisitor;
import pt.up.fe.comp.jmm.ast.JmmNode;
import pt.up.fe.comp.jmm.analysis.table.Type;

import java.util.ArrayList;
import java.util.List;

public class ConstructSymbolTableVisitor extends AJmmVisitor<ConcreteSymbolTable, Void> {
    public ConstructSymbolTableVisitor() {
        addVisit("Program", this::visitProgram);
        addVisit("ImportDeclaration", this::visitImport);
        addVisit("ClassDeclaration", this::visitClassDeclaration);
        addVisit("MethodDef", this::visitMethod);
        addVisit("MainMethodDef", this::visitMethod);
    }

    private Void visitProgram(JmmNode jmmNode, ConcreteSymbolTable table) {
        for (JmmNode child: jmmNode.getChildren()) {
            visit(child, table);
        }

        return null;
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
        if (jmmNode.getAttributes().contains("extends")) {
            table.setSuper(jmmNode.get("extends"));
        }

        for (JmmNode child: jmmNode.getChildren()) {
            if (child.getKind().equals("VarDeclaration")) {
                table.addField(getSymbol(child));
            } else {
                visit(child, table);
            }
        }

        return null;
    }

    private Void visitMethod(JmmNode jmmNode, ConcreteSymbolTable table) {
        String methodName = jmmNode.get("name");
        Type returnType = getType(jmmNode);
        List<Symbol> parameters = new ArrayList<>();
        List<Symbol> variables = new ArrayList<>();

        for (JmmNode child: jmmNode.getChildren()) {
            if (child.getKind().equals("Parameter")) {
                parameters.add(getSymbol(child));
            } else if (child.getKind().equals("MethodBody")) {
                for (JmmNode grandchild: child.getChildren()) {
                    if (grandchild.getKind().equals("VarDeclaration")) {
                        variables.add(getSymbol(grandchild));
                    }
                }
            }
        }

        String methodSignature = table.addMethod(methodName, returnType, parameters, variables);
        jmmNode.put("signature", methodSignature);
        return null;
    }

    private Type getType(JmmNode jmmNode) {
        JmmNode child = jmmNode.getJmmChild(0);
        boolean isArray = child.getAttributes().contains("isArray") && child.get("isArray").equals("true");
        return new Type(child.get("type"), isArray);
    }

    private Symbol getSymbol(JmmNode jmmNode) {
        String name = jmmNode.get("name");
        Type type = getType(jmmNode);
        return new Symbol(type, name);
    }
}

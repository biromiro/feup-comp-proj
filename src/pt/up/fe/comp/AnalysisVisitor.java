package pt.up.fe.comp;

import pt.up.fe.comp.jmm.analysis.table.Symbol;
import pt.up.fe.comp.jmm.ast.AJmmVisitor;
import pt.up.fe.comp.jmm.ast.JmmNode;
import pt.up.fe.comp.jmm.analysis.table.Type;

import java.util.ArrayList;
import java.util.List;

public class AnalysisVisitor extends AJmmVisitor<ConcreteSymbolTable, Void> {
    public AnalysisVisitor() {
        addVisit("Import", this::visitImport);
        addVisit("ClassDeclaration", this::visitClassDeclaration);
        addVisit("VarDeclaration", this::visitVarDeclaration);
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

        return null;
    }

    private Void visitVarDeclaration(JmmNode jmmNode, ConcreteSymbolTable table) {
        String name = jmmNode.get("name");
        JmmNode child = jmmNode.getJmmChild(0);
        Type type = new Type(child.get("type"), child.get("isArray") == "true");
        Symbol symbol = new Symbol(type, name);

        if (jmmNode.getJmmParent().getKind() == "ClassDeclaration") {
            table.addField(symbol);
        } else {
            // TODO add method info
        }

        return null;
    }

    private Void visitMethod(JmmNode jmmNode, ConcreteSymbolTable table) {
        String methodName = jmmNode.get("name");
        JmmNode typeNode = jmmNode.getJmmChild(0);
        Type returnType = new Type(typeNode.get("type"), typeNode.get("isArray") == "true");
        List<Symbol> parameters = new ArrayList<>();

        for (JmmNode child: jmmNode.getChildren()) {
            if (child.getKind() == "Parameter") {
                // parameters.add(child.get("name"));
            }
        }

        table.addMethod(methodName, returnType, parameters);
        return null;
    }
}

package pt.up.fe.comp.analysis;

import pt.up.fe.comp.jmm.analysis.table.Symbol;
import pt.up.fe.comp.jmm.ast.AJmmVisitor;
import pt.up.fe.comp.jmm.ast.JmmNode;
import pt.up.fe.comp.jmm.analysis.table.Type;
import pt.up.fe.comp.jmm.report.Report;
import pt.up.fe.comp.jmm.report.ReportType;
import pt.up.fe.comp.jmm.report.Stage;
import pt.up.fe.specs.util.SpecsCollections;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class ConstructSymbolTableVisitor extends AJmmVisitor<ConcreteSymbolTable, List<Report>> {
    public ConstructSymbolTableVisitor() {
        addVisit("Program", this::visitProgram);
        addVisit("ImportDeclaration", this::visitImport);
        addVisit("ClassDeclaration", this::visitClassDeclaration);
        addVisit("MethodDef", this::visitMethod);
        addVisit("MainMethodDef", this::visitMethod);
    }

    private void visitAndReduce(JmmNode jmmNode, ConcreteSymbolTable table, List<Report> reports) {
        reports.addAll(visit(jmmNode, table));
    }

    private List<Report> visitProgram(JmmNode jmmNode, ConcreteSymbolTable table) {
        List<Report> reports = new ArrayList<>();

        for (JmmNode child: jmmNode.getChildren()) {
            visitAndReduce(child, table, reports);
        }

        return reports;
    }

    private List<Report> visitImport(JmmNode jmmNode, ConcreteSymbolTable table) {
        List<Report> reports = new ArrayList<>();
        // TODO what should happen if two different imports to the same classname (e.g. import math.Vector and import graphics.Vector)
        StringBuilder importPath = new StringBuilder();
        for (JmmNode child: jmmNode.getChildren()) {
            importPath.append(child.get("path"));
            importPath.append('.');
        }

        importPath.deleteCharAt(importPath.length()-1);
        if (!table.getImports().contains(importPath.toString())) {
            table.addImport(importPath.toString());
        }

        return reports;
    }

    private List<Report> visitClassDeclaration(JmmNode jmmNode, ConcreteSymbolTable table) {
        List<Report> reports = new ArrayList<>();

        table.setClassName(jmmNode.get("classname"));
        if (jmmNode.getAttributes().contains("extends")) {
            table.setSuper(jmmNode.get("extends"));
        }

        for (JmmNode child: jmmNode.getChildren()) {
            if (child.getKind().equals("VarDeclaration")) {
                Symbol symbol = getSymbol(child);
                if (table.getFields().contains(symbol)) {
                    reports.add(variableAlreadyDefinedReport(symbol.getName(), table.getClassName()));
                } else {
                    table.addField(symbol);
                }
            } else {
                visit(child, table);
            }
        }

        return reports;
    }

    //private void visitMethod(JmmNode jmmNode, ConcreteSymbolTable table) {

    //}

    private List<Report> visitMethod(JmmNode jmmNode, ConcreteSymbolTable table) {
        // TODO errors
        List<Report> reports = new ArrayList<>();

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
        return reports;
    }

    static private Report variableAlreadyDefinedReport(String variable, String className) {
        StringBuilder message = new StringBuilder();
        message.append("variable ");
        message.append(variable);
        message.append(" is already defined in class ");
        message.append(className);

        return new Report(ReportType.ERROR, Stage.SEMANTIC, -1, message.toString());
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

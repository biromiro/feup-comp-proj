package pt.up.fe.comp.analysis;

import pt.up.fe.comp.jmm.analysis.table.Symbol;
import pt.up.fe.comp.jmm.ast.AJmmVisitor;
import pt.up.fe.comp.jmm.ast.JmmNode;
import pt.up.fe.comp.jmm.analysis.table.Type;
import pt.up.fe.comp.jmm.report.Report;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

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
                Symbol symbol = AnalysisUtils.getSymbol(child);

                if (table.getFields().stream().anyMatch(s -> s.getName().equals(symbol.getName()))) {
                    reports.add(ReportUtils.symbolAlreadyDefinedReport(child, "variable", symbol.getName(), "class", table.getClassName()));
                } else {
                    table.addField(symbol);
                }
            } else {
                visitAndReduce(child, table, reports);
            }
        }

        return reports;
    }

    private List<Report> visitMethod(JmmNode jmmNode, ConcreteSymbolTable table) {
        List<Report> reports = new ArrayList<>();

        String methodName = jmmNode.get("name");
        Type returnType = AnalysisUtils.getType(jmmNode.getJmmChild(0));
        List<Symbol> parameters = new ArrayList<>();
        List<Symbol> variables = new ArrayList<>();

        for (JmmNode child: jmmNode.getChildren()) {
            if (child.getKind().equals("Parameter")) {
                Symbol symbol = AnalysisUtils.getSymbol(child);

                if (parameters.stream().anyMatch(s -> s.getName().equals(symbol.getName()))) {
                    reports.add(ReportUtils.symbolAlreadyDefinedReport(child , "variable", symbol.getName(), "method", methodName));
                } else {
                    parameters.add(symbol);
                }
            } else if (child.getKind().equals("MethodBody")) {
                for (JmmNode grandchild: child.getChildren()) {
                    if (grandchild.getKind().equals("VarDeclaration")) {
                        Symbol symbol = AnalysisUtils.getSymbol(grandchild);

                        if (Stream.concat(variables.stream(), parameters.stream()).anyMatch(s -> s.getName().equals(symbol.getName()))) {
                            reports.add(ReportUtils.symbolAlreadyDefinedReport(grandchild, "variable", symbol.getName(), "method", methodName));
                        } else {
                            variables.add(symbol);
                        }
                    }
                }
            }
        }

        String methodSignature = AnalysisUtils.getMethodSignature(methodName, parameters);

        if (table.getMethods().contains(methodSignature)) {
            String symbolName = AnalysisUtils.getMethodSymbolName(methodSignature);
            reports.add(ReportUtils.symbolAlreadyDefinedReport(jmmNode, "method", symbolName, "class", table.getClassName()));
        } else {
            table.addMethod(methodSignature, returnType, parameters, variables);
        }

        jmmNode.put("signature", methodSignature);

        return reports;
    }
}

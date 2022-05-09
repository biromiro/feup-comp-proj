package pt.up.fe.comp.analysis;

import java.util.Collections;
import java.util.List;

import pt.up.fe.comp.jmm.analysis.JmmAnalysis;
import pt.up.fe.comp.jmm.analysis.JmmSemanticsResult;
import pt.up.fe.comp.jmm.ast.JmmNode;
import pt.up.fe.comp.jmm.parser.JmmParserResult;
import pt.up.fe.comp.jmm.report.Report;
import pt.up.fe.specs.util.SpecsCollections;

public class Analyser implements JmmAnalysis {

    @Override
    public JmmSemanticsResult semanticAnalysis(JmmParserResult parserResult) {
        ConcreteSymbolTable symbolTable = new ConcreteSymbolTable();

        List<Report> symbolTableReports = new ConstructSymbolTableVisitor().visit(parserResult.getRootNode(), symbolTable);
        List<Report> analysisReports = new AnalyseVisitor().visit(parserResult.getRootNode(), symbolTable);
        List<Report> reports = SpecsCollections.concat(symbolTableReports, analysisReports);

        return new JmmSemanticsResult(parserResult, symbolTable, reports);
    }
}
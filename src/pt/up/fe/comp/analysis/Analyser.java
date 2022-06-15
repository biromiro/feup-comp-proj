package pt.up.fe.comp.analysis;

import pt.up.fe.comp.jmm.analysis.JmmAnalysis;
import pt.up.fe.comp.jmm.analysis.JmmSemanticsResult;
import pt.up.fe.comp.jmm.parser.JmmParserResult;
import pt.up.fe.comp.jmm.report.Report;
import pt.up.fe.specs.util.SpecsCollections;

import java.util.List;

public class Analyser implements JmmAnalysis {

    @Override
    public JmmSemanticsResult semanticAnalysis(JmmParserResult parserResult) {
        ConcreteSymbolTable symbolTable = new ConcreteSymbolTable();

        List<Report> symbolTableReports = new ConstructSymbolTableVisitor().visit(parserResult.getRootNode(), symbolTable);
        List<Report> analysisReports = new AnalyseVisitor().visit(parserResult.getRootNode(), symbolTable);
        List<Report> reports = SpecsCollections.concat(symbolTableReports, analysisReports);

        if (parserResult.getConfig().getOrDefault("debug", "false").equals("true")) {
            System.out.println("AST:\n");
            System.out.println(parserResult.getRootNode().toTree());

            System.out.println("Symbol table:\n");
            symbolTable.print();
        }

        return new JmmSemanticsResult(parserResult, symbolTable, reports);
    }
}
package pt.up.fe.comp.analysis;

import java.util.Collections;
import pt.up.fe.comp.jmm.analysis.JmmAnalysis;
import pt.up.fe.comp.jmm.analysis.JmmSemanticsResult;
import pt.up.fe.comp.jmm.parser.JmmParserResult;

public class Analyser implements JmmAnalysis {

    @Override
    public JmmSemanticsResult semanticAnalysis(JmmParserResult parserResult) {
        ConcreteSymbolTable symbolTable = new ConcreteSymbolTable();
        new ConstructSymbolTableVisitor().visit(parserResult.getRootNode(), symbolTable);

        return new JmmSemanticsResult(parserResult, symbolTable, Collections.emptyList());
    }
}
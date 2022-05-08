package pt.up.fe.comp.ollir;

import pt.up.fe.comp.jmm.analysis.table.Symbol;
import pt.up.fe.comp.jmm.analysis.table.SymbolTable;
import pt.up.fe.comp.jmm.ast.AJmmVisitor;
import pt.up.fe.comp.jmm.ast.JmmNode;

import java.util.List;
import java.util.stream.Collectors;

public class OllirGenerator extends AJmmVisitor<Integer, Integer> {
    private final StringBuilder ollirCode;
    private final SymbolTable symbolTable;

    public OllirGenerator(SymbolTable symbolTable) {
        this.ollirCode = new StringBuilder();
        this.symbolTable = symbolTable;

        addVisit("Program", this::programVisit);
        addVisit("ClassDeclaration", this::classDeclarationVisit);
        addVisit("MainMethodDef", this::methodDefVisit);
        addVisit("MethodDef", this::methodDefVisit);
        addVisit("MethodCall", this::methodCallVisit);
        addVisit("Identifier", this::identifierVisit);
        addVisit("MethodArguments", this::methodArgumentsVisit);
    }

    public String getCode() {
        return ollirCode.toString();
    }


    private Integer programVisit(JmmNode program, Integer dummy) {

        for(String importString: symbolTable.getImports()) {
            ollirCode.append("import ").append(importString).append(";\n");
        }

        for(JmmNode child: program.getChildren()) {
            visit(child);
        }
        return 0;
    }

    private Integer classDeclarationVisit(JmmNode classDeclaration, Integer dummy) {

        ollirCode.append("public ").append(symbolTable.getClassName());

        String superClass = symbolTable.getSuper();

        if(superClass != null) {
            ollirCode.append(" extends ").append(superClass);
        }

        ollirCode.append(" {\n");

        for(JmmNode child: classDeclaration.getChildren()) {
            visit(child);
        }

        ollirCode.append("}\n");

        return 0;
    }

    private Integer methodDefVisit(JmmNode method, Integer dummy) {

        String methodSignature = method.get("signature");

        ollirCode.append(".method public ");

        if(method.getKind().equals("MainMethodDef")) ollirCode.append("static ");

        ollirCode.append(method.get("name") + "(");

        List<Symbol> parameters = symbolTable.getParameters(methodSignature);

        String parametersCode = parameters.stream()
                .map(symbol -> OllirUtils.getCode(symbol))
                .collect(Collectors.joining(", "));

        ollirCode.append(parametersCode)
                .append(").")
                .append(OllirUtils.getCode(symbolTable.getReturnType(methodSignature)));

        ollirCode.append(" {\n");

        for (JmmNode methodChild: method.getChildren()) {
            if (methodChild.getKind().equals("MethodBody")) {
                for(JmmNode child: methodChild.getChildren()) {
                    System.out.println(child.getKind());
                    visit(child);
                }
                break;
            }
        }

        ollirCode.append("}\n");

        return 0;
    }

    private Integer methodCallVisit(JmmNode method, Integer dummy) {

        // TODO: verify type of the first parameter (static or virtual)
        // TODO: exprtoollir -> code before, value (ollirCode.append(codebefore).append(invokestatic(value..))
        // TODO: verify return type of the called class

        ollirCode.append("invokestatic(").append(method.getJmmChild(0).get("name"))
                .append(", \"").append(method.get("methodname")).append("\")")
                .append(".V")
                .append(";\n");

        // TODO: conditional visitor for arguments

        return 0;
    }

    private Integer methodArgumentsVisit(JmmNode arguments, Integer dummy) {

        for(JmmNode child: arguments.getChildren()) {
            ollirCode.append(", ");
            visit(child);
        }

        return 0;
    }

    private Integer identifierVisit(JmmNode identifier, Integer dummy) {

        ollirCode.append(identifier.get("name"));

        return 0;
    }


}

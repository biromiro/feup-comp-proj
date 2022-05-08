package pt.up.fe.comp.ollir;

import pt.up.fe.comp.analysis.AnalysisUtils;
import pt.up.fe.comp.jmm.analysis.table.Symbol;
import pt.up.fe.comp.jmm.analysis.table.SymbolTable;
import pt.up.fe.comp.jmm.analysis.table.Type;
import pt.up.fe.comp.jmm.ast.AJmmVisitor;
import pt.up.fe.comp.jmm.ast.JmmNode;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

public class OllirGenerator extends AJmmVisitor<String, String> {
    private final StringBuilder ollirCode;
    private final SymbolTable symbolTable;
    private int temporaryVarCounter = 0;


    public OllirGenerator(SymbolTable symbolTable) {
        this.ollirCode = new StringBuilder();
        this.symbolTable = symbolTable;

        addVisit("Program", this::programVisit);
        addVisit("ClassDeclaration", this::classDeclarationVisit);
        addVisit("MainMethodDef", this::methodDefVisit);
        addVisit("MethodDef", this::methodDefVisit);
        addVisit("VarDeclaration", this::varDeclarationVisit);
        addVisit("MethodCall", this::methodCallVisit);
        addVisit("Identifier", this::identifierVisit);
        addVisit("MethodArguments", this::methodArgumentsVisit);
        addVisit("Assignment", this::assignmentVisit);
        addVisit("IntegerLiteral", this::terminalVisit);
        addVisit("BooleanLiteral", this::terminalVisit);
        addVisit("BinaryOp", this::binaryOpVisit);
        addVisit("ReturnStatement", this::returnStatementVisit);
    }

    public String getCode() {
        return ollirCode.toString();
    }

    private String getNextTemp(Type type) {
        StringBuilder temp = new StringBuilder();
        temp.append(temporaryVarCounter++);
        return OllirUtils.getTempCode(temp.toString(), type);
    }

    private String programVisit(JmmNode program, String dummy) {
        for(String importString: symbolTable.getImports()) {
            ollirCode.append("import ").append(importString).append(";\n");
        }

        for(JmmNode child: program.getChildren()) {
            visit(child);
        }
        return "";
    }

    private String emptyConstructor() {
        StringBuilder emptyConstructor = new StringBuilder();

        emptyConstructor.append(".construct ")
                .append(symbolTable.getClassName())
                .append("().V {\n")
                .append("invokespecial(this, \"<init>\").V;\n")
                .append("}\n\n");

        return emptyConstructor.toString();
    }

    private String classDeclarationVisit(JmmNode classDeclaration, String dummy) {

        ollirCode.append(symbolTable.getClassName());

        String superClass = symbolTable.getSuper();

        if(superClass != null) {
            ollirCode.append(" extends ").append(superClass);
        }

        ollirCode.append(" {\n");

        boolean insertedConstructor = false;

        for(JmmNode child: classDeclaration.getChildren()) {

            if (!child.getKind().equals("VarDeclaration")
            && !insertedConstructor) {
                ollirCode.append(emptyConstructor());
                insertedConstructor = true;
            }

            visit(child);
        }

        ollirCode.append("}\n");

        return "";
    }

    private String methodDefVisit(JmmNode method, String dummy) {

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
                    visit(child);
                }
                break;
            }
        }

        ollirCode.append("}\n\n");

        return "";
    }

    private String varDeclarationVisit(JmmNode variableDeclaration, String dummy) {

        // TODO: check if private or package private

        if(!variableDeclaration.getJmmParent().getKind().equals("ClassDeclaration"))
            return "";

        Type type = AnalysisUtils.getType(variableDeclaration.getJmmChild(0));

        ollirCode.append(".field private ")
                .append(OllirUtils.getCode(
                        new Symbol(type, variableDeclaration.get("name"))
                )).append(";\n");

        return "";
    }

    private String assignmentVisit(JmmNode assignment, String dummy) {

        String methodSignature = assignment
                .getAncestor("MethodDef")
                .or(() -> assignment.getAncestor("MainMethodDef"))
                .get()
                .get("signature");

        Symbol variable = symbolTable.getLocalVariables(methodSignature).stream()
                .filter(localVar -> assignment.getJmmChild(0).get("name").equals(localVar.getName()))
                .findAny()
                .orElse(null);

        if (assignment.getNumChildren() > 0) {
            String rhs = visit(assignment.getJmmChild(1));
            JmmNode identifier = assignment.getJmmChild(0);
            // TODO: check local vars first, then check class vars

            ollirCode.append(OllirUtils.getCode(variable))
                    .append(" :=.")
                    .append(OllirUtils.getCode(variable.getType()))
                    .append(" ")
                    .append(rhs)
                    .append(";\n");

        } else {

        }

        return "";
    }

    private String binaryOpVisit(JmmNode jmmNode, String s) {

        String lhs = visit(jmmNode.getJmmChild(0));
        String rhs = visit(jmmNode.getJmmChild(1));
        Type tempType = AnalysisUtils.getType(jmmNode);
        String temp =  getNextTemp(tempType);

        ollirCode.append(temp)
                .append(" :=.")
                .append(OllirUtils.getCode(tempType))
                .append(" ")
                .append(lhs)
                .append(" ").append(jmmNode.get("op"))
                .append(".")
                .append(OllirUtils.getCode(tempType))
                .append(" ")
                .append(rhs)
                .append(";\n");

        return temp;
    }

    private String terminalVisit(JmmNode terminalNode, String s) {
        return OllirUtils.getCode(terminalNode.get("val"),
                AnalysisUtils.getType(terminalNode));
    }

    private String returnStatementVisit(JmmNode jmmNode, String s) {
        JmmNode returnNode = jmmNode.getJmmChild(0);
        Type returnType = AnalysisUtils.getType(returnNode);
        String returnVal = visit(returnNode);

        ollirCode.append("ret.")
                .append(OllirUtils.getCode(returnType))
                .append(" ")
                .append(returnVal)
                .append(";\n");

        return "";
    }


    private String methodCallVisit(JmmNode method, String dummy) {

        // TODO: verify type of the first parameter (static or virtual)
        // TODO: exprtoollir -> code before, value (ollirCode.append(codebefore).append(invokestatic(value..))
        // TODO: verify return type of the called class

        System.out.println("Called method visit\n");

        JmmNode identifier = method.getJmmChild(0);
        JmmNode methodArguments = method.getJmmChild(1);
        Optional<String> type = identifier.getOptional("type");

        if (type.isPresent()) {
            ollirCode.append("invokevirtual(");
        } else if (type.isEmpty()) {
            ollirCode.append("invokestatic(");
        }

        ollirCode.append(identifier.get("name"))
                .append(", \"").append(method.get("methodname")).append("\"");

        visit(methodArguments);

        ollirCode.append(").")
                .append(OllirUtils.getCode(AnalysisUtils.getType(method)))
                .append(";\n");


        // TODO: conditional visitor for arguments

        return "";
    }

    private String methodArgumentsVisit(JmmNode arguments, String dummy) {

        for(JmmNode child: arguments.getChildren()) {
            ollirCode.append(", ").append(visit(child));
        }

        return "";
    }

    private String identifierVisit(JmmNode identifier, String dummy) {

        String methodSignature = identifier
                .getAncestor("MethodDef")
                .or(() -> identifier.getAncestor("MainMethodDef"))
                .get()
                .get("signature");

        int index = symbolTable.getParameters(methodSignature).indexOf(
                new Symbol(AnalysisUtils.getType(identifier), identifier.get("name"))
        );

        StringBuilder parameterPrefix = new StringBuilder();

        if (index != -1) {

            parameterPrefix.append("$").append(index + 1).append(".");
        }

        return parameterPrefix.toString() + OllirUtils.getCode(
                new Symbol(AnalysisUtils.getType(identifier),
                        identifier.get("name")
                ));
    }


}

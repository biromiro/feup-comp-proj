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
        addVisit("NewIntArray", this::newIntArrayVisit);
        addVisit("LengthCall", this::lengthCallVisit);
        addVisit("Indexing", this::indexingVisit);
        addVisit("NewObject", this::newObjectVisit);
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

        JmmNode identifier = assignment.getJmmChild(0);
        String lhs = visit(identifier);
        String rhs = visit(assignment.getJmmChild(1));

        ollirCode.append(lhs)
                .append(" :=.")
                .append(OllirUtils.getCode(AnalysisUtils.getType(identifier)))
                .append(" ")
                .append(rhs)
                .append(";\n");

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

        JmmNode identifier = method.getJmmChild(0);
        JmmNode methodArguments = method.getJmmChild(1);
        Optional<String> type = identifier.getOptional("type");

        if (type.isPresent()) {
            ollirCode.append("invokevirtual(");
        } else if (type.isEmpty()) {
            ollirCode.append("invokestatic(");
        }

        ollirCode.append(visit(identifier))
                .append(", \"").append(method.get("methodname")).append("\"");

        visit(methodArguments);

        ollirCode.append(").")
                .append(OllirUtils.getCode(AnalysisUtils.getType(method)))
                .append(";\n");

        return "";
    }

    private String methodArgumentsVisit(JmmNode arguments, String dummy) {

        for(JmmNode child: arguments.getChildren()) {
            ollirCode.append(", ").append(visit(child));
        }

        return "";
    }


    private String newIntArrayVisit(JmmNode jmmNode, String s) {

        String arrVal = visit(jmmNode.getJmmChild(0));
        StringBuilder newIntArray = new StringBuilder();
        Type arrayType = AnalysisUtils.getType(jmmNode);

        newIntArray.append("new(array,")
                .append(arrVal)
                .append(").")
                .append(OllirUtils.getCode(arrayType));

        return newIntArray.toString();

    }

    private String lengthCallVisit(JmmNode jmmNode, String s) {

        String callee = visit(jmmNode.getJmmChild(0));
        Type callType = AnalysisUtils.getType(jmmNode);
        String temp =  getNextTemp(callType);

        ollirCode.append(temp)
                .append(" :=.")
                .append(OllirUtils.getCode(callType))
                .append(" ")
                .append("arraylength(")
                .append(callee)
                .append(").")
                .append(OllirUtils.getCode(callType))
                .append(";\n");

        return temp;

    }


    private String indexingVisit(JmmNode jmmNode, String s) {

        JmmNode identifier = jmmNode.getJmmChild(0);
        String indexValue = visit(jmmNode.getJmmChild(1));
        Type indexType = AnalysisUtils.getType(jmmNode);

        String rhs = getNextTemp(indexType);

        ollirCode.append(rhs)
                .append(" :=.")
                .append(OllirUtils.getCode(indexType))
                .append(" ")
                .append(indexValue)
                .append(";\n");

        return visit(identifier, rhs);
    }

    private String newObjectVisit(JmmNode jmmNode, String s) {

        StringBuilder newObj = new StringBuilder();
        Type objType = AnalysisUtils.getType(jmmNode);

        newObj.append("new(")
                .append(jmmNode.get("type"))
                .append(").")
                .append(OllirUtils.getCode(objType));


        return newObj.toString();

    }

    private String identifierVisit(JmmNode identifier, String indexing) {

        Optional<String> type = identifier.getOptional("type");

        if (type.isEmpty()) return identifier.get("name");

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

        if (indexing != null && !indexing.isEmpty()) {
            return parameterPrefix.toString() + OllirUtils.getCode(
                    new Symbol(AnalysisUtils.getType(identifier),
                            identifier.get("name")
                    ), indexing);
        }

        return parameterPrefix.toString() + OllirUtils.getCode(
                new Symbol(AnalysisUtils.getType(identifier),
                        identifier.get("name")
                ));
    }


}

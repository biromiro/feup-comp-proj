package pt.up.fe.comp.ollir;

import pt.up.fe.comp.analysis.AnalysisUtils;
import pt.up.fe.comp.jmm.analysis.table.Symbol;
import pt.up.fe.comp.jmm.analysis.table.SymbolTable;
import pt.up.fe.comp.jmm.analysis.table.Type;
import pt.up.fe.comp.jmm.ast.AJmmVisitor;
import pt.up.fe.comp.jmm.ast.JmmNode;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

public class OllirGenerator extends AJmmVisitor<Option, String> {
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
        addVisit("NotExpression", this::notExpressionVisit);
        addVisit("ThisKeyword", this::thisKeywordVisit);
    }

    public String getCode() {
        return ollirCode.toString();
    }

    private Symbol getNextTempSymbol(Type type) {
        StringBuilder temp = new StringBuilder();
        temp.append(temporaryVarCounter++);
        return new Symbol(type, temp.toString());
    }

    private String getNextTemp(Type type) {
        StringBuilder temp = new StringBuilder();
        temp.append(temporaryVarCounter++);
        return OllirUtils.getTempCode(temp.toString(), type);
    }

    private String programVisit(JmmNode program, Option option) {
        for(String importString: symbolTable.getImports()) {
            ollirCode.append("import ").append(importString).append(";\n");
        }

        for(JmmNode child: program.getChildren()) {
            visit(child, new Option());
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

    private String classDeclarationVisit(JmmNode classDeclaration, Option option) {

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

            visit(child, option);
        }

        ollirCode.append("}\n");

        return "";
    }

    private String methodDefVisit(JmmNode method, Option option) {

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
                    visit(child, option);
                }
                break;
            }
        }

        ollirCode.append("}\n\n");

        return "";
    }

    private String varDeclarationVisit(JmmNode variableDeclaration, Option option) {

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

    private String assignmentVisit(JmmNode assignment, Option option) {

        JmmNode identifier = assignment.getJmmChild(0);
        String lhs = visit(identifier, option);
        String rhs = visit(assignment.getJmmChild(1), new Option(TypeDecl.ASSIGNMENT));

        ollirCode.append(lhs)
                .append(" :=.")
                .append(OllirUtils.getCode(AnalysisUtils.getType(identifier)))
                .append(" ")
                .append(rhs)
                .append(";\n");

        return "";
    }

    private String binaryOpVisit(JmmNode jmmNode, Option option) {

        String lhs = visit(jmmNode.getJmmChild(0), option);
        String rhs = visit(jmmNode.getJmmChild(1), option);
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

    private String terminalVisit(JmmNode terminalNode, Option option) {
        return OllirUtils.getCode(terminalNode.get("val"),
                AnalysisUtils.getType(terminalNode));
    }

    private String returnStatementVisit(JmmNode jmmNode, Option option) {
        JmmNode returnNode = jmmNode.getJmmChild(0);
        Type returnType = AnalysisUtils.getType(returnNode);
        String returnVal = visit(returnNode, option);

        ollirCode.append("ret.")
                .append(OllirUtils.getCode(returnType))
                .append(" ")
                .append(returnVal)
                .append(";\n");

        return "";
    }


    private String methodCallVisit(JmmNode method, Option option) {

        JmmNode identifier = method.getJmmChild(0);
        JmmNode methodArguments = method.getJmmChild(1);
        Optional<String> type = identifier.getOptional("type");
        StringBuilder call = new StringBuilder();
        String id = visit(identifier, new Option(TypeDecl.ARGUMENT));
        String args = visit(methodArguments, option);

        if (type.isPresent()) {
            call.append("invokevirtual(");
        } else if (type.isEmpty()) {
            call.append("invokestatic(");
        }


        call.append(id)
                .append(", \"").append(method.get("methodname")).append("\"")
                .append(args)
                .append(").")
                .append(OllirUtils.getCode(AnalysisUtils.getType(method)));

        if (option.getType() == TypeDecl.ASSIGNMENT) {
            return call.toString();

        } else if (option.getType() == TypeDecl.ARGUMENT) {
            Type callType = AnalysisUtils.getType(method);
            String temp =  getNextTemp(callType);

            ollirCode.append(temp)
                    .append(" :=.")
                    .append(OllirUtils.getCode(callType))
                    .append(" ")
                    .append(call.toString())
                    .append(";\n");

            return temp;
        } else if (option.getType() == TypeDecl.INDEXING) {

            Type callType = AnalysisUtils.getType(method);
            Symbol tempSymbol = getNextTempSymbol(callType);
            String temp = OllirUtils.getTempCode(tempSymbol.getName(), tempSymbol.getType());

            ollirCode.append(temp)
                    .append(" :=.")
                    .append(OllirUtils.getCode(callType))
                    .append(" ")
                    .append(call.toString())
                    .append(";\n");

            return OllirUtils.getTempCode(tempSymbol, option.getContext());
        }

        ollirCode.append(call.toString())
                .append(";\n");
        return "";
    }

    private String methodArgumentsVisit(JmmNode arguments, Option typeOfCall) {

        List<String> string_args = new ArrayList<>();
        StringBuilder args = new StringBuilder();

        for(JmmNode child: arguments.getChildren()) {
            string_args.add(visit(child, new Option(TypeDecl.ARGUMENT)));
        }

        for(String arg: string_args) {
            args.append(", ").append(arg);
        }

        return args.toString();
    }


    private String newIntArrayVisit(JmmNode jmmNode, Option option) {

        String arrVal = visit(jmmNode.getJmmChild(0), option);
        StringBuilder newIntArray = new StringBuilder();
        Type arrayType = AnalysisUtils.getType(jmmNode);

        newIntArray.append("new(array,")
                .append(arrVal)
                .append(").")
                .append(OllirUtils.getCode(arrayType));

        return newIntArray.toString();

    }

    private String lengthCallVisit(JmmNode jmmNode, Option option) {

        String callee = visit(jmmNode.getJmmChild(0), option);
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


    private String indexingVisit(JmmNode jmmNode, Option option) {

        JmmNode identifier = jmmNode.getJmmChild(0);
        String indexValue = visit(jmmNode.getJmmChild(1), option);
        Type indexType = AnalysisUtils.getType(jmmNode);

        String indexTemp = getNextTemp(indexType);

        ollirCode.append(indexTemp)
                .append(" :=.")
                .append(OllirUtils.getCode(indexType))
                .append(" ")
                .append(indexValue)
                .append(";\n");

        return visit(identifier, new Option(TypeDecl.INDEXING, indexTemp));
    }

    private String newObjectVisit(JmmNode jmmNode, Option option) {

        StringBuilder newObj = new StringBuilder();
        Type objType = AnalysisUtils.getType(jmmNode);

        newObj.append("new(")
                .append(jmmNode.get("type"))
                .append(").")
                .append(OllirUtils.getCode(objType));

        if (option.getType() == TypeDecl.ARGUMENT) {
            Type callType = AnalysisUtils.getType(jmmNode);
            String temp =  getNextTemp(callType);

            ollirCode.append(temp)
                    .append(" :=.")
                    .append(OllirUtils.getCode(callType))
                    .append(" ")
                    .append(newObj)
                    .append(";\n");

            return temp;
        }

        return newObj.toString();

    }

    private String identifierVisit(JmmNode identifier, Option indexing) {

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

        if (indexing.getType() == TypeDecl.INDEXING) {
            return parameterPrefix.toString() + OllirUtils.getCode(
                    new Symbol(AnalysisUtils.getType(identifier),
                            identifier.get("name")
                    ), indexing.getContext());
        }

        return parameterPrefix.toString() + OllirUtils.getCode(
                new Symbol(AnalysisUtils.getType(identifier),
                        identifier.get("name")
                ));
    }

    private String notExpressionVisit(JmmNode jmmNode, Option option) {
        String callee = visit(jmmNode.getJmmChild(0), option);
        Type callType = AnalysisUtils.getType(jmmNode);
        String temp =  getNextTemp(callType);

        ollirCode.append(temp)
                .append(" :=.")
                .append(OllirUtils.getCode(callType))
                .append(" ")
                .append("!.")
                .append(OllirUtils.getCode(callType))
                .append(" ")
                .append(callee)
                .append(".")
                .append(OllirUtils.getCode(callType))
                .append(";\n");

        return temp;
    }

    private String thisKeywordVisit(JmmNode jmmNode, Option s) {
        return "this";
    }

}

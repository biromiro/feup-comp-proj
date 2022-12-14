package pt.up.fe.comp.ollir;

import pt.up.fe.comp.NotExpression;
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

public class OllirGenerator extends AJmmVisitor<Action, String> {
    private final StringBuilder ollirCode;
    private final SymbolTable symbolTable;
    private int temporaryVarCounter = 0;
    private int labelCounter = 0;

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
        addVisit("IfThenElseStatement", this::ifThenElseStatementVisit);
        addVisit("ScopeStatement", this::scopeStatementVisit);
        addVisit("WhileStatement", this::whileStatementVisit);
    }

    public String getCode() {
        return ollirCode.toString();
    }

    private String getNextLabel(LabelType label) {
        int labelVal = labelCounter++;
        switch (label) {
            case THEN -> {
                return "THEN_" + labelVal;
            }
            case ENDIF -> {
                return "ENDIF_" + labelVal;
            }
            case LOOP ->  {
                return "LOOP_" + labelVal;
            }
            case BODY -> {
                return "BODY_" + labelVal;
            }
            case ENDLOOP -> {
                return "ENDLOOP_" + labelVal;
            }
        }

        return "NULL_" + labelVal;
    }

    private Symbol getNextTempSymbol(Type type) {
        return new Symbol(type, String.valueOf(temporaryVarCounter++));
    }

    private String getNextTempIndexed(Type type) {
        return OllirUtils.getTempCodeIndexed(String.valueOf(temporaryVarCounter++), type);
    }

    private String getNextTemp(Type type) {
        return OllirUtils.getTempCode(String.valueOf(temporaryVarCounter++), type);
    }

    private String programVisit(JmmNode program, Action action) {
        for(String importString: symbolTable.getImports()) {
            ollirCode.append("import ").append(importString).append(";\n");
        }

        for(JmmNode child: program.getChildren()) {
            visit(child, new Action());
        }
        return "";
    }

    private String emptyConstructor() {

        final String emptyConstructor = ".construct " +
                symbolTable.getClassName() +
                "().V {\n" +
                "invokespecial(this, \"<init>\").V;\n" +
                "}\n\n";
        return emptyConstructor;
    }

    private boolean isLocalVariable(JmmNode identifier) {
        if (!identifier.getKind().equals("Identifier")) return false;

        String methodSignature = identifier
                .getAncestor("MethodDef")
                .or(() -> identifier.getAncestor("MainMethodDef"))
                .get()
                .get("signature");

        return symbolTable.getLocalVariables(methodSignature).
                contains(new Symbol(AnalysisUtils.getType(identifier), identifier.get("name")));
    }

    private int getArgumentVariableIndex(JmmNode identifier) {
        if (!identifier.getKind().equals("Identifier")
                && !isLocalVariable(identifier)) return -1;

        String methodSignature = identifier
                .getAncestor("MethodDef")
                .or(() -> identifier.getAncestor("MainMethodDef"))
                .get()
                .get("signature");

        return symbolTable.getParameters(methodSignature).indexOf(
                new Symbol(AnalysisUtils.getType(identifier), identifier.get("name"))
                );
    }

    private boolean isClassVariable(JmmNode identifier) {

        if (!identifier.getKind().equals("Identifier")) return false;

        int index = symbolTable.getFields().indexOf(
                new Symbol(AnalysisUtils.getType(identifier), identifier.get("name"))
        );

        return !isLocalVariable(identifier) &&
                getArgumentVariableIndex(identifier) == -1 &&
                index != -1;

    }

    private String classDeclarationVisit(JmmNode classDeclaration, Action action) {

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

            visit(child, action);
        }

        ollirCode.append("}\n");

        return "";
    }

    private String methodDefVisit(JmmNode method, Action action) {

        String methodSignature = method.get("signature");

        ollirCode.append(".method public ");

        if(method.getKind().equals("MainMethodDef")) ollirCode.append("static ");

        ollirCode.append(method.get("name")).append("(");

        List<Symbol> parameters = symbolTable.getParameters(methodSignature);

        String parametersCode = parameters.stream()
                .map(OllirUtils::getCode)
                .collect(Collectors.joining(", "));

        ollirCode.append(parametersCode)
                .append(").")
                .append(OllirUtils.getCode(symbolTable.getReturnType(methodSignature)));

        ollirCode.append(" {\n");

        for (JmmNode methodChild: method.getChildren()) {
            if (methodChild.getKind().equals("MethodBody")) {
                for(JmmNode child: methodChild.getChildren()) {
                    visit(child, action);
                }
                break;
            }
        }

        if (method.getKind().equals("MainMethodDef")) {
            ollirCode.append("ret.V;\n");
        }

        ollirCode.append("}\n\n");

        return "";
    }

    private String varDeclarationVisit(JmmNode variableDeclaration, Action action) {

        if(!variableDeclaration.getJmmParent().getKind().equals("ClassDeclaration"))
            return "";

        Type type = AnalysisUtils.getType(variableDeclaration.getJmmChild(0));

        ollirCode.append(".field private ")
                .append(OllirUtils.getCode(
                        new Symbol(type, variableDeclaration.get("name"))
                )).append(";\n");

        return "";
    }

    private String assignmentVisit(JmmNode assignment, Action action) {

        JmmNode identifier = assignment.getJmmChild(0);

        if (isClassVariable(identifier)) {
            String lhs = visit(identifier, new Action(ActionType.ASSIGN_TO_FIELD));
            String rhs = visit(assignment.getJmmChild(1), new Action(ActionType.SAVE_TO_TMP));
            ollirCode.append("putfield(this,")
                    .append(lhs)
                    .append(",")
                    .append(rhs)
                    .append(").V;\n");
        } else {
            String lhs = visit(identifier, action);
            String rhs = visit(assignment.getJmmChild(1), new Action(ActionType.RET_VAL));
            ollirCode.append(lhs)
                    .append(" :=.")
                    .append(OllirUtils.getCode(AnalysisUtils.getType(identifier)))
                    .append(" ")
                    .append(rhs)
                    .append(";\n");
        }

        return "";
    }

    private String binaryOpVisit(JmmNode jmmNode, Action action) {

        String lhs = visit(jmmNode.getJmmChild(0), new Action(ActionType.SAVE_TO_TMP));
        String rhs = visit(jmmNode.getJmmChild(1), new Action(ActionType.SAVE_TO_TMP));
        Type tempType = AnalysisUtils.getType(jmmNode);
        String temp =  getNextTemp(tempType);

        if (action.getAction() == ActionType.RET_VAL) {
            return lhs + " " + jmmNode.get("op") + "." + OllirUtils.getCode(tempType) + " " + rhs;
        }

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

    private String terminalVisit(JmmNode terminalNode, Action action) {
        return OllirUtils.getCode(terminalNode.get("val"),
                AnalysisUtils.getType(terminalNode));
    }

    private String returnStatementVisit(JmmNode jmmNode, Action action) {
        JmmNode returnNode = jmmNode.getJmmChild(0);
        Type returnType = AnalysisUtils.getType(returnNode);
        String returnVal = visit(returnNode, new Action(ActionType.SAVE_TO_TMP));

        if (returnNode.getKind().equals("ThisKeyword")) {
            ollirCode.append("ret.")
                    .append(OllirUtils.getCode(returnType))
                    .append(" ")
                    .append(returnVal)
                    .append(".").append(OllirUtils.getCode(returnType))
                    .append(";\n");
        } else {
            ollirCode.append("ret.")
                    .append(OllirUtils.getCode(returnType))
                    .append(" ")
                    .append(returnVal)
                    .append(";\n");
        }

        return "";
    }


    private String methodCallVisit(JmmNode method, Action action) {

        JmmNode identifier = method.getJmmChild(0);
        JmmNode methodArguments = method.getJmmChild(1);
        Optional<String> type = identifier.getOptional("type");
        StringBuilder call = new StringBuilder();
        String id = visit(identifier, new Action(ActionType.SAVE_TO_TMP));
        String args = visit(methodArguments, action);

        if (id.charAt(0) == '$') {
            Type identifierType = AnalysisUtils.getType(identifier);

            String temp =  getNextTemp(identifierType);

            ollirCode.append(temp)
                    .append(" :=.")
                    .append(OllirUtils.getCode(identifierType))
                    .append(" ")
                    .append(id)
                    .append(";\n");

            id = temp;
        }

        if (type.isPresent()) {
            call.append("invokevirtual(");
        } else {
            call.append("invokestatic(");
        }


        call.append(id)
                .append(", \"").append(method.get("methodname")).append("\"")
                .append(args)
                .append(").")
                .append(OllirUtils.getCode(AnalysisUtils.getType(method)));

        // method call needs to be safe to temp
        if (action.getAction() == ActionType.RET_VAL || action.getAction() == ActionType.SAVE_TO_TMP) {
            Type callType = AnalysisUtils.getType(method);
            String temp =  getNextTemp(callType);

            ollirCode.append(temp)
                    .append(" :=.")
                    .append(OllirUtils.getCode(callType))
                    .append(" ")
                    .append(call.toString())
                    .append(";\n");

            return temp;
        } else if (action.getAction() == ActionType.SAVE_FOR_IDX) {
            Type callType = AnalysisUtils.getType(method);
            Symbol tempSymbol = getNextTempSymbol(callType);
            String temp = OllirUtils.getTempCode(tempSymbol.getName(), tempSymbol.getType());

            ollirCode.append(temp)
                    .append(" :=.")
                    .append(OllirUtils.getCode(callType))
                    .append(" ")
                    .append(call.toString())
                    .append(";\n");

            return OllirUtils.getTempCode(tempSymbol, action.getContext());

        }

        ollirCode.append(call)
                .append(";\n");
        return "";
    }

    private String methodArgumentsVisit(JmmNode arguments, Action typeOfCall) {

        List<String> string_args = new ArrayList<>();
        StringBuilder args = new StringBuilder();

        for(JmmNode child: arguments.getChildren()) {
            string_args.add(visit(child, new Action(ActionType.SAVE_TO_TMP)));
        }

        for(String arg: string_args) {
            args.append(", ").append(arg);
        }

        return args.toString();
    }


    private String newIntArrayVisit(JmmNode jmmNode, Action action) {

        String arrVal = visit(jmmNode.getJmmChild(0), new Action(ActionType.SAVE_TO_TMP));
        StringBuilder newIntArray = new StringBuilder();
        Type arrayType = AnalysisUtils.getType(jmmNode);

        newIntArray.append("new(array,")
                .append(arrVal)
                .append(").")
                .append(OllirUtils.getCode(arrayType));

        if (action.getAction() != ActionType.SAVE_TO_TMP) {
            return newIntArray.toString();
        }

        String temp =  getNextTemp(arrayType);
        ollirCode.append(temp)
                .append(" :=.")
                .append(OllirUtils.getCode(arrayType))
                .append(" ")
                .append(newIntArray)
                .append(";\n");

        return temp;

    }

    private String lengthCallVisit(JmmNode jmmNode, Action action) {

        String callee = visit(jmmNode.getJmmChild(0), action);
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


    private String indexingVisit(JmmNode jmmNode, Action action) {

        JmmNode identifier = jmmNode.getJmmChild(0);
        String indexValue = visit(jmmNode.getJmmChild(1), action);
        Type indexType = AnalysisUtils.getType(jmmNode);

        Symbol tempSymbol = getNextTempSymbol(indexType);
        String indexTemp = OllirUtils.getTempCode(tempSymbol.getName(), tempSymbol.getType());

        ollirCode.append(indexTemp)
                .append(" :=.")
                .append(OllirUtils.getCode(indexType))
                .append(" ")
                .append(indexValue)
                .append(";\n");

        if (identifier.getKind().equals("Identifier")) {
            if (action.getAction() == ActionType.SAVE_TO_TMP)
                return visit(identifier, new Action(ActionType.SAVE_ARR_TMP, indexTemp));
            else return visit(identifier, new Action(ActionType.SAVE_FOR_IDX, indexTemp));
        }

        String temp = visit(identifier, new Action(ActionType.SAVE_TO_TMP));
        Type identifierType = AnalysisUtils.getType(identifier);
        String temp2 = getNextTempIndexed(identifierType);

        ollirCode.append(temp2)
                .append(" :=.")
                .append(OllirUtils.getCode(identifierType, true))
                .append(" ")
                .append(getName(temp))
                .append("[")
                .append(indexTemp)
                .append("].")
                .append(OllirUtils.getCode(identifierType, true))
                .append(";\n");

        /*if (action.getAction() == ActionType.SAVE_TO_TMP) {

            String temp2 =  getNextTempIndexed(identifierType);

            ollirCode.append(temp2)
                    .append(" :=.")
                    .append(OllirUtils.getCode(identifierType, true))
                    .append(" ")
                    .append(temp)
                    .append(";\n");

            return temp2;
        }*/

        return temp2;

    }

    private String getName(String temp) {
        return temp.split("\\.",2)[0];
    }

    private String newObjectVisit(JmmNode jmmNode, Action action) {

        StringBuilder newObj = new StringBuilder();
        Type objType = AnalysisUtils.getType(jmmNode);

        newObj.append("new(")
                .append(jmmNode.get("type"))
                .append(").")
                .append(OllirUtils.getCode(objType));

        Type callType = AnalysisUtils.getType(jmmNode);
        String temp =  getNextTemp(callType);

        ollirCode.append(temp)
                .append(" :=.")
                .append(OllirUtils.getCode(callType))
                .append(" ")
                .append(newObj)
                .append(";\n");

        ollirCode.append("invokespecial(")
                .append(temp)
                .append(",\"<init>\").V;\n");

        return temp;

    }

    private String identifierVisit(JmmNode identifier, Action action) {

        Optional<String> type = identifier.getOptional("type");

        if (type.isEmpty()) return identifier.get("name");

        StringBuilder parameterPrefix = new StringBuilder();

        int index = getArgumentVariableIndex(identifier);

        if (index != -1) {
            parameterPrefix.append("$").append(index + 1).append(".");
        }

        if (isClassVariable(identifier) && action.getAction() != ActionType.ASSIGN_TO_FIELD) {
            Type identifierType = AnalysisUtils.getType(identifier);
            String identifierVal = OllirUtils.getCode(
                    new Symbol(AnalysisUtils.getType(identifier),
                            identifier.get("name")
                    ));

            String temp =  getNextTemp(identifierType);

            ollirCode.append(temp)
                    .append(" :=.")
                    .append(OllirUtils.getCode(identifierType))
                    .append(" getfield(this, ")
                    .append(identifierVal)
                    .append(").")
                    .append(OllirUtils.getCode(identifierType))
                    .append(";\n");

            return temp;
        }

        if (action.getAction() != ActionType.SAVE_FOR_IDX
        && action.getAction() != ActionType.SAVE_ARR_TMP) {
            return parameterPrefix + OllirUtils.getCode(
                    new Symbol(AnalysisUtils.getType(identifier),
                            identifier.get("name")
                    ));
        }
        Type arrayValType = AnalysisUtils.getType(identifier);
        String arrayAccess = parameterPrefix + OllirUtils.getCode(
                new Symbol(arrayValType, identifier.get("name")),
                action.getContext()
        );

        if (action.getAction() == ActionType.SAVE_FOR_IDX) {
            return arrayAccess;
        }

        String temp =  getNextTempIndexed(arrayValType);

        ollirCode.append(temp)
                .append(" :=.")
                .append(OllirUtils.getCode(arrayValType, true))
                .append(" ")
                .append(arrayAccess)
                .append(";\n");

        return temp;

    }

    private String notExpressionVisit(JmmNode jmmNode, Action action) {

        Type callType = AnalysisUtils.getType(jmmNode);

        if (action.getAction() == ActionType.RET_VAL) {
            String callee = visit(jmmNode.getJmmChild(0), new Action(ActionType.SAVE_TO_TMP));
            return "!." + OllirUtils.getCode(callType) + " " + callee;
        }

        String callee = visit(jmmNode.getJmmChild(0), action);
        String temp =  getNextTemp(callType);
        ollirCode.append(temp)
                .append(" :=.")
                .append(OllirUtils.getCode(callType))
                .append(" ")
                .append("!.")
                .append(OllirUtils.getCode(callType))
                .append(" ")
                .append(callee)
                .append(";\n");

        return temp;
    }

    private String thisKeywordVisit(JmmNode jmmNode, Action s) {
        return "this";
    }

    private String ifThenElseStatementVisit(JmmNode jmmNode, Action action) {

        String condition = visit(jmmNode.getJmmChild(0), new Action(ActionType.RET_VAL));
        String thenLabel = getNextLabel(LabelType.THEN);
        String endifLabel = getNextLabel(LabelType.ENDIF);

        JmmNode thenNode = jmmNode.getJmmChild(1);
        JmmNode elseNode = jmmNode.getJmmChild(2);

        ollirCode.append("if (")
                .append(condition)
                .append(") goto ")
                .append(thenLabel)
                .append(";\n");

        visit(elseNode, action);

        ollirCode.append("goto ")
                .append(endifLabel)
                .append(";\n")
                .append(thenLabel)
                .append(":\n");

        visit(thenNode, action);

        ollirCode.append(endifLabel)
                .append(":\n");

        return "";
    }

    private String scopeStatementVisit(JmmNode jmmNode, Action action) {
        for (JmmNode child : jmmNode.getChildren()) {
            visit(child, action);
        }
        return "";
    }

    private String whileStatementVisit(JmmNode jmmNode, Action action) {
        Optional<String> type = jmmNode.getOptional("dowhile");
        String bodyLabel = getNextLabel(LabelType.BODY);
        JmmNode conditionNode = jmmNode.getJmmChild(0);
        JmmNode bodyNode = jmmNode.getJmmChild(1);

        if (type.isPresent()) {
            ollirCode.append(bodyLabel)
                    .append(":\n");

            visit(bodyNode, action);
            String condition = visit(conditionNode, new Action(ActionType.RET_VAL));
            ollirCode.append("if (")
                    .append(condition)
                    .append(") goto ")
                    .append(bodyLabel)
                    .append(";\n");
            return "";
        }
        String endLoopLabel = getNextLabel(LabelType.ENDLOOP);
        String oppositeOp = null;
        Optional<String> op = conditionNode.getOptional("op");

        if (conditionNode.getKind().equals("BinaryOp") && op.isPresent()) {
            oppositeOp = switch (op.get()) {
                case "<" -> ">=";
                case ">=" -> "<";
                default -> null;
            };

        }
        if (oppositeOp != null) {
            conditionNode.put("op", oppositeOp);
            String condition = visit(conditionNode, new Action(ActionType.RET_VAL));
            ollirCode.append("if (")
                    .append(condition)
                    .append(") goto ")
                    .append(endLoopLabel)
                    .append(";\n")
                    .append(bodyLabel)
                    .append(":\n");
            conditionNode.put("op", op.get());
        } else if (conditionNode.getKind().equals("NotExpression")) {
            String condition = visit(conditionNode.getJmmChild(0), new Action(ActionType.RET_VAL));
            ollirCode.append("if (")
                    .append(condition)
                    .append(") goto ")
                    .append(endLoopLabel)
                    .append(";\n")
                    .append(bodyLabel)
                    .append(":\n");
        } else {
            String conditionToNegate = visit(conditionNode, new Action(ActionType.SAVE_TO_TMP));
            Type callType = AnalysisUtils.getType(conditionNode);

            ollirCode.append("if (")
                    .append("!.")
                    .append(OllirUtils.getCode(callType))
                    .append(" ")
                    .append(conditionToNegate)
                    .append(") goto ")
                    .append(endLoopLabel)
                    .append(";\n")
                    .append(bodyLabel)
                    .append(":\n");
        }



        visit(bodyNode, action);
        String condition = visit(conditionNode, new Action(ActionType.RET_VAL));

        ollirCode.append("if (")
                .append(condition)
                .append(") goto ")
                .append(bodyLabel)
                .append(";\n")
                .append(endLoopLabel)
                .append(":\n");

        return "";
    }

}

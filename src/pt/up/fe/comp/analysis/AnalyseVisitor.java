package pt.up.fe.comp.analysis;

import org.eclipse.jgit.util.StringUtils;
import pt.up.fe.comp.jmm.analysis.table.Symbol;
import pt.up.fe.comp.jmm.analysis.table.SymbolTable;
import pt.up.fe.comp.jmm.analysis.table.Type;
import pt.up.fe.comp.jmm.ast.JmmNode;
import pt.up.fe.comp.jmm.ast.PostorderJmmVisitor;
import pt.up.fe.comp.jmm.report.Report;
import pt.up.fe.comp.jmm.report.ReportType;
import pt.up.fe.comp.jmm.report.Stage;
import pt.up.fe.specs.util.SpecsCollections;

import java.util.*;

// TODO better error messages and line/column numbers?
public class AnalyseVisitor extends PostorderJmmVisitor<SymbolTable, List<Report>> {
    static final List<String> PRIMITIVES = Arrays.asList("int", "void", "boolean");
    static final List<String> ARITHMETIC_OP = Arrays.asList("ADD", "SUB", "MUL", "DIV");
    static final List<String> COMPARISON_OP = Arrays.asList("LT");
    static final List<String> LOGICAL_OP = Arrays.asList("AND");


    public AnalyseVisitor() {
        addVisit("Type", this::visitType);
        addVisit("ReturnStatement", this::visitReturn);
        addVisit("IfThenElseStatement", this::visitCondition);
        addVisit("WhileStatement", this::visitCondition);
        addVisit("Assignment", this::visitAssignment);
        addVisit("BinaryOp", this::visitBinaryOp);
        addVisit("NotExpression", this::visitNot);
        addVisit("NewIntArray", this::visitNewIntArray);
        addVisit("NewObject", this::visitNewObject);
        addVisit("Indexing", this::visitIndexing);
        addVisit("LengthCall", this::visitLengthCall);
        addVisit("MethodCall", this::visitMethodCall);
        addVisit("ThisKeyword", this::visitThisKeyword);
        addVisit("Identifier", this::visitIdentifier);

        setDefaultValue(Collections::emptyList);
        setReduceSimple(this::reduceReports);
    }

    private List<Report> reduceReports(List<Report> reports1, List<Report> reports2) {
        return SpecsCollections.concatList(reports1, reports2);
    }

    private List<Report> visitIdentifier(JmmNode jmmNode, SymbolTable symbolTable) {
        List<Report> reports = new ArrayList<>();

        Symbol identifier = getSymbol(jmmNode);

        String methodSignature = jmmNode
                .getAncestor("MethodDef")
                .or(() -> jmmNode.getAncestor("MainMethodDef"))
                .get()
                .get("signature");

        Optional<Symbol> parameterSymbol = symbolTable
                .getParameters(methodSignature)
                .stream()
                .filter(p -> p.equals(identifier))
                .findFirst();

        if (parameterSymbol.isPresent()) {
            putType(jmmNode, parameterSymbol.get().getType());
            return reports;
        }

        Optional<Symbol> localVarSymbol = symbolTable
                .getLocalVariables(methodSignature)
                .stream()
                .filter(l -> l.equals(identifier))
                .findFirst();

        if (localVarSymbol.isPresent()) {
            putType(jmmNode, localVarSymbol.get().getType());
            return reports;
        }

        Optional<Symbol> fieldSymbol = symbolTable
                .getFields()
                .stream()
                .filter(f -> f.equals(identifier))
                .findFirst();

        if (fieldSymbol.isPresent()) {
            putType(jmmNode, fieldSymbol.get().getType());
            return reports;
        }

        reports.add(cannotFindSymbolReport(identifier.getName()));

        return reports;
    }

    private List<Report> visitThisKeyword(JmmNode jmmNode, SymbolTable symbolTable) {
        putType(jmmNode, new Type(symbolTable.getClassName(), false));
        return new ArrayList<>();
    }

    private List<Report> visitMethodCall(JmmNode jmmNode, SymbolTable symbolTable) {
        List<Report> reports = new ArrayList<>();

        Type operand = getType(jmmNode.getJmmChild(0));
        if (PRIMITIVES.contains(operand.getName())) {
            reports.add(cannotBeDereferencedReport(operand.getName()));
            return reports;
        }

        if (operand.isArray() || !symbolTable.getClassName().equals(operand.getName())) {
            return reports;
        }

        StringBuilder methodSignatureBuilder = new StringBuilder();
        methodSignatureBuilder.append(jmmNode.get("methodname"));
        for (JmmNode argument: jmmNode.getJmmChild(1).getChildren()) {
            Type argType = getType(argument);
            methodSignatureBuilder.append("#");
            methodSignatureBuilder.append(argType.print());
        }

        String methodSignature = methodSignatureBuilder.toString();
        if (symbolTable.getMethods().contains(methodSignature)) {
            Type returnType = symbolTable.getReturnType(methodSignature);
            putType(jmmNode, returnType);
        } else {
            if (symbolTable.getSuper() == null) {
                String symbolName = methodSignature.replaceFirst("#", "(")
                        .replaceAll("#", ",")
                        .concat(")");

                reports.add(cannotFindSymbolReport(symbolName));
            }
        }

        return reports;
    }

    private List<Report> visitLengthCall(JmmNode jmmNode, SymbolTable symbolTable) {
        List<Report> reports = new ArrayList<>();

        Type type = getType(jmmNode.getJmmChild(0));
        if (!type.isArray()) {
            reports.add(arrayRequiredReport(type.print()));
        }

        putType(jmmNode, new Type("int", false));

        return reports;
    }

    private List<Report> visitIndexing(JmmNode jmmNode, SymbolTable symbolTable) {
        List<Report> reports = new ArrayList<>();

        Type operandType = getType(jmmNode.getJmmChild(0));
        Type indexType = getType(jmmNode.getJmmChild(1));

        if (!indexType.getName().equals("int") || indexType.isArray()) {
            reports.add(incompatibleTypesReport(indexType.print(), "int"));
        }

        if (!operandType.isArray()) {
            reports.add(arrayRequiredReport(operandType.print()));
        }

        putType(jmmNode, new Type(operandType.getName(), false)); // TODO what if error?

        return reports;
    }

    private List<Report> checkImport(JmmNode jmmNode, SymbolTable symbolTable) {
        List<Report> reports = new ArrayList<>();

        Type objType = getType(jmmNode);

        if (symbolTable.getClassName().equals(objType.getName()) || objType.getName().equals("String")) {
            jmmNode.put("qualifiedname", objType.getName());
            return reports;
        }

        Optional<String> qualifiedName = symbolTable
                .getImports()
                .stream()
                .filter(imp -> imp.substring(imp.lastIndexOf(".") + 1).equals(objType.getName()))
                .findAny();

        if (qualifiedName.isPresent()) {
            jmmNode.put("qualifiedname", qualifiedName.get());
            return reports;
        }

        reports.add(cannotFindSymbolReport(objType.print()));

        return reports;
    }

    private List<Report> visitNewObject(JmmNode jmmNode, SymbolTable symbolTable) {
        List<Report> reports = new ArrayList<>();

        Type objType = getType(jmmNode);
        if (objType.isArray() || PRIMITIVES.contains(objType.getName())) {
            reports.add(operatorCannotBeAppliedReport("new", objType.print()));
            return reports;
        }

        return checkImport(jmmNode, symbolTable);
    }

    private List<Report> visitNewIntArray(JmmNode jmmNode, SymbolTable symbolTable) {
        List<Report> reports = new ArrayList<>();

        Type type = getType(jmmNode.getJmmChild(0));
        if (!type.getName().equals("int") && !type.isArray()) {
            reports.add(incompatibleTypesReport(type.print(), "int"));
        }

        putType(jmmNode, new Type("int", true));

        return reports;
    }

    private List<Report> visitNot(JmmNode jmmNode, SymbolTable symbolTable) {
        List<Report> reports = new ArrayList<>();
        Type type = getType(jmmNode.getJmmChild(0));

        if (type.isArray() || !type.getName().equals("boolean")) {
            reports.add(operatorCannotBeAppliedReport("!", type.print(), "boolean"));
        }

        putType(jmmNode, type); // TODO what if error?

        return reports;
    }

    private List<Report> visitBinaryOp(JmmNode jmmNode, SymbolTable symbolTable) {
        List<Report> reports = new ArrayList<>();
        Type lhsType = getType(jmmNode.getJmmChild(0));
        Type rhsType = getType(jmmNode.getJmmChild(1));

        if (!lhsType.equals(rhsType)) {
            reports.add(incompatibleTypesReport(rhsType.print(), lhsType.print()));
        } else if (ARITHMETIC_OP.contains(jmmNode.get("op")) || LOGICAL_OP.contains(jmmNode.get("op"))) {
            if (lhsType.isArray()) {
                reports.add(operatorCannotBeAppliedReport(jmmNode.get("op"), lhsType.print(), rhsType.print()));
            }
        } else if (ARITHMETIC_OP.contains(jmmNode.get("op")) || COMPARISON_OP.contains(jmmNode.get("op"))) {
            if (!lhsType.getName().equals("int")) {
                reports.add(operatorCannotBeAppliedReport(jmmNode.get("op"), lhsType.print(), rhsType.print()));
            }
        } else if (LOGICAL_OP.contains(jmmNode.get("op"))) {
            if (!lhsType.getName().equals("boolean")) {
                reports.add(operatorCannotBeAppliedReport(jmmNode.get("op"), lhsType.print(), rhsType.print()));
            }
        } else if (!PRIMITIVES.contains(lhsType.getName())) {
            reports.add(operatorCannotBeAppliedReport(jmmNode.get("op"), lhsType.print(), rhsType.print()));
        }

        putType(jmmNode, lhsType); // TODO what if error?

        return reports;
    }

    private void putType(JmmNode jmmNode, Type type) {
        jmmNode.put("type", type.getName());
        jmmNode.put("isArray", String.valueOf(type.isArray()));
    }

    private List<Report> visitAssignment(JmmNode jmmNode, SymbolTable symbolTable) {
        List<Report> reports = new ArrayList<>();
        Type lhsType = getType(jmmNode.getJmmChild(0));
        Type rhsType = getType(jmmNode.getJmmChild(1));

        if (!lhsType.equals(rhsType)) {
            reports.add(incompatibleTypesReport(rhsType.print(), lhsType.print()));
        }

        putType(jmmNode, lhsType); // TODO what if error?

        return reports;
    }

    static private Report cannotBeDereferencedReport(String type) {
        StringBuilder message = new StringBuilder();
        message.append("incompatible types: ");
        message.append(type);
        message.append(" cannot be dereferenced");
        return new Report(ReportType.ERROR, Stage.SEMANTIC, -1, message.toString());
    }

    static private Report incompatibleTypesReport(String actual, String expected) {
        StringBuilder message = new StringBuilder();
        message.append("incompatible types: ");
        message.append(actual);
        message.append(" cannot be converted to ");
        message.append(expected);
        return new Report(ReportType.ERROR, Stage.SEMANTIC, -1, message.toString());
    }

    static private Report operatorCannotBeAppliedReport(String operator, String lhs, String rhs) {
        StringBuilder message = new StringBuilder();
        message.append("operator '");
        message.append(operator);
        message.append("' cannot be applied to '");
        message.append(lhs);
        message.append("' and '");
        message.append(rhs);
        message.append("'");

        return new Report(ReportType.ERROR, Stage.SEMANTIC, -1, message.toString());
    }

    static private Report operatorCannotBeAppliedReport(String operator, String lhs) {
        StringBuilder message = new StringBuilder();
        message.append("operator '");
        message.append(operator);
        message.append("' cannot be applied to '");
        message.append(lhs);
        message.append("'");

        return new Report(ReportType.ERROR, Stage.SEMANTIC, -1, message.toString());
    }

    static private Report arrayRequiredReport(String found) {
        StringBuilder message = new StringBuilder();
        message.append("array required, but ");
        message.append(found);
        message.append(" found");

        return new Report(ReportType.ERROR, Stage.SEMANTIC, -1, message.toString());
    }

    static private Report cannotFindSymbolReport(String symbol) {
        StringBuilder message = new StringBuilder();
        message.append("cannot find symbol '");
        message.append(symbol);
        message.append("'");

        return new Report(ReportType.ERROR, Stage.SEMANTIC, -1, message.toString());
    }
    private List<Report> visitCondition(JmmNode jmmNode, SymbolTable symbolTable) {
        List<Report> reports = new ArrayList<>();
        Type conditionType = getType(jmmNode.getJmmChild(0));

        if (conditionType.isArray() || !conditionType.getName().equals("boolean")) {
            reports.add(incompatibleTypesReport(conditionType.print(), "boolean"));
        }

        return reports;
    }

    private List<Report> visitReturn(JmmNode jmmNode, SymbolTable symbolTable) {
        List<Report> reports = new ArrayList<>();
        String methodSignature = jmmNode.getJmmParent().getJmmParent().get("signature");
        Type returnType = symbolTable.getReturnType(methodSignature);
        Type expressionType = getType(jmmNode.getJmmChild(0));

        if (!expressionType.equals(returnType)) {
            reports.add(incompatibleTypesReport(expressionType.print(), returnType.print()));
        }

        return reports;
    }

    private List<Report> visitType(JmmNode jmmNode, SymbolTable symbolTable) {
        List<Report> reports = new ArrayList<>();

        Type objType = getType(jmmNode);
        if (PRIMITIVES.contains(objType.getName())) {
            return reports;
        }

        return checkImport(jmmNode, symbolTable);
    }

    private Type getType(JmmNode jmmNode) {
        boolean isArray = jmmNode.getAttributes().contains("isArray") && jmmNode.get("isArray").equals("true");
        return new Type(jmmNode.get("type"), isArray);
    }

    private Symbol getSymbol(JmmNode jmmNode) {
        String name = jmmNode.get("name");
        Type type = getType(jmmNode);
        return new Symbol(type, name);
    }
}

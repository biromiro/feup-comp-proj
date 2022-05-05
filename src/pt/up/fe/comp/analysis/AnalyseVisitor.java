package pt.up.fe.comp.analysis;

import pt.up.fe.comp.jmm.analysis.table.Symbol;
import pt.up.fe.comp.jmm.analysis.table.SymbolTable;
import pt.up.fe.comp.jmm.analysis.table.Type;
import pt.up.fe.comp.jmm.ast.JmmNode;
import pt.up.fe.comp.jmm.ast.PostorderJmmVisitor;
import pt.up.fe.comp.jmm.report.Report;
import pt.up.fe.specs.util.SpecsCollections;

import java.util.*;

public class AnalyseVisitor extends PostorderJmmVisitor<SymbolTable, List<Report>> {
    static final List<String> PRIMITIVES = Arrays.asList("int", "void", "boolean");
    static final List<String> ARITHMETIC_OP = Arrays.asList("ADD", "SUB", "MUL", "DIV");
    static final List<String> COMPARISON_OP = Arrays.asList("LT");
    static final List<String> LOGICAL_OP = Arrays.asList("AND");


    public AnalyseVisitor() {
        addVisit("ClassDeclaration", this::visitClassDeclaration);
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

    private List<Report> visitClassDeclaration(JmmNode jmmNode, SymbolTable symbolTable) {
        List<Report> reports = new ArrayList<>();

        if (!jmmNode.getAttributes().contains("extends")) {
            return reports;
        }

        String extendsName = jmmNode.get("extends");

        if (jmmNode.get("classname").equals(extendsName)) {
            reports.add(ReportUtils.cyclicInheritance(jmmNode, extendsName));
            return reports;
        }

        boolean foundImport = symbolTable
                .getImports()
                .stream()
                .anyMatch(imp -> imp.substring(imp.lastIndexOf(".") + 1).equals(extendsName));

        if (foundImport) {
            return reports;
        }

        reports.add(ReportUtils.cannotFindSymbolReport(jmmNode, extendsName));

        return reports;
    }

    private List<Report> visitIdentifier(JmmNode jmmNode, SymbolTable symbolTable) {
        List<Report> reports = new ArrayList<>();

        String identifier = jmmNode.get("name");

        String methodSignature = jmmNode
                .getAncestor("MethodDef")
                .or(() -> jmmNode.getAncestor("MainMethodDef"))
                .get()
                .get("signature");

        Optional<Symbol> parameterSymbol = symbolTable
                .getParameters(methodSignature)
                .stream()
                .filter(p -> p.getName().equals(identifier))
                .findFirst();

        if (parameterSymbol.isPresent()) {
            putType(jmmNode, parameterSymbol.get().getType());
            return reports;
        }

        Optional<Symbol> localVarSymbol = symbolTable
                .getLocalVariables(methodSignature)
                .stream()
                .filter(l -> l.getName().equals(identifier))
                .findFirst();

        if (localVarSymbol.isPresent()) {
            putType(jmmNode, localVarSymbol.get().getType());
            return reports;
        }

        Optional<Symbol> fieldSymbol = symbolTable
                .getFields()
                .stream()
                .filter(f -> f.getName().equals(identifier))
                .findFirst();

        if (fieldSymbol.isPresent()) {
            putType(jmmNode, fieldSymbol.get().getType());
            return reports;
        }

        if (!jmmNode.getJmmParent().getKind().equals("MethodCall")
                || (!identifier.equals("String") && !symbolTable.getClassName().equals(identifier) && !symbolTable.getImports().contains(identifier))) {
            putUnknownType(jmmNode);
            reports.add(ReportUtils.cannotFindSymbolReport(jmmNode, identifier));
        }

        return reports;
    }

    private List<Report> visitThisKeyword(JmmNode jmmNode, SymbolTable symbolTable) {
        putType(jmmNode, new Type(symbolTable.getClassName(), false));
        return new ArrayList<>();
    }

    private List<Report> visitMethodCall(JmmNode jmmNode, SymbolTable symbolTable) {
        List<Report> reports = new ArrayList<>();

        if (!jmmNode.getJmmChild(0).getAttributes().contains("type")) {
            if (jmmNode.getJmmChild(0).get("name").equals(symbolTable.getClassName())) {
                String methodSignature = inferMethodSignature(jmmNode);
                boolean methodExists;
                if (methodSignature.contains("##UNKNOWN")) {
                    methodExists = symbolTable
                            .getMethods()
                            .stream()
                            .anyMatch(m -> m.substring(0, m.indexOf("#")).equals(jmmNode.get("methodname")));
                } else {
                    methodExists = symbolTable
                            .getMethods()
                            .contains(methodSignature);
                }
                
                if (methodExists) {
                    String symbolName = AnalysisUtils.getMethodSymbolName(methodSignature);
                    reports.add(ReportUtils.nonStaticInStaticContext(jmmNode, symbolName));
                } else if (symbolTable.getSuper() == null) {
                    String symbolName = AnalysisUtils.getMethodSymbolName(methodSignature);
                    reports.add(ReportUtils.cannotFindSymbolReport(jmmNode, symbolName));
                }
            }
            putUnknownType(jmmNode);
            return reports;
        }

        Type operand = AnalysisUtils.getType(jmmNode.getJmmChild(0));
        if (PRIMITIVES.contains(operand.getName())) {
            putUnknownType(jmmNode);
            reports.add(ReportUtils.cannotBeDereferencedReport(jmmNode, operand.getName()));
            return reports;
        }

        if (operand.getName().equals("#UNKNOWN") || operand.isArray() || !symbolTable.getClassName().equals(operand.getName())) {
            putUnknownType(jmmNode);
            return reports;
        }

        String methodSignature = inferMethodSignature(jmmNode);
        if (symbolTable.getMethods().contains(methodSignature)) {
            Type returnType = symbolTable.getReturnType(methodSignature);
            putType(jmmNode, returnType);
        } else {
            boolean incompleteSignature = symbolTable
                    .getMethods()
                    .stream()
                    .anyMatch(m -> m.substring(0, m.indexOf("#")).equals(jmmNode.get("methodname")));
            if (!incompleteSignature && symbolTable.getSuper() == null) {
                String symbolName = AnalysisUtils.getMethodSymbolName(methodSignature);
                reports.add(ReportUtils.cannotFindSymbolReport(jmmNode, symbolName));
            }
            putUnknownType(jmmNode); // TODO this might give problems if not checked for the restriction in page 2 of the assignment
        }

        return reports;
    }

    private String inferMethodSignature(JmmNode jmmNode) {
        StringBuilder methodSignatureBuilder = new StringBuilder();
        methodSignatureBuilder.append(jmmNode.get("methodname"));
        for (JmmNode argument: jmmNode.getJmmChild(1).getChildren()) {
            Type argType = AnalysisUtils.getType(argument);
            methodSignatureBuilder.append("#");
            methodSignatureBuilder.append(argType.print());
        }
        return methodSignatureBuilder.toString();
    }

    private List<Report> visitLengthCall(JmmNode jmmNode, SymbolTable symbolTable) {
        List<Report> reports = new ArrayList<>();

        Type type = AnalysisUtils.getType(jmmNode.getJmmChild(0));
        if (!type.getName().equals("#UNKNOWN") && !type.isArray()) {
            reports.add(ReportUtils.arrayRequiredReport(jmmNode, type.print()));
        }

        putType(jmmNode, new Type("int", false));

        return reports;
    }

    private List<Report> visitIndexing(JmmNode jmmNode, SymbolTable symbolTable) {
        List<Report> reports = new ArrayList<>();

        Type operandType = AnalysisUtils.getType(jmmNode.getJmmChild(0));
        Type indexType = AnalysisUtils.getType(jmmNode.getJmmChild(1));

        if (!indexType.getName().equals("#UNKNOWN") && (!indexType.getName().equals("int") || indexType.isArray())) {
            reports.add(ReportUtils.incompatibleTypesReport(jmmNode.getJmmChild(1), indexType.print(), "int"));
        }

        if (operandType.getName().equals("#UNKNOWN")) {
            putUnknownType(jmmNode);
        } else if (operandType.isArray()) {
            putType(jmmNode, new Type(operandType.getName(), false));
        } else {
            putUnknownType(jmmNode);
            reports.add(ReportUtils.arrayRequiredReport(jmmNode.getJmmChild(0), operandType.print()));
        }

        return reports;
    }

    private List<Report> visitNewObject(JmmNode jmmNode, SymbolTable symbolTable) {
        List<Report> reports = new ArrayList<>();

        Type objType = AnalysisUtils.getType(jmmNode);
        if (objType.isArray() || PRIMITIVES.contains(objType.getName())) {
            putUnknownType(jmmNode);
            reports.add(ReportUtils.operatorCannotBeAppliedReport(jmmNode, "new", objType.print()));
            return reports;
        }

        return checkTypeImport(jmmNode, symbolTable);
    }

    private List<Report> visitNewIntArray(JmmNode jmmNode, SymbolTable symbolTable) {
        List<Report> reports = new ArrayList<>();

        Type type = AnalysisUtils.getType(jmmNode.getJmmChild(0));
        if (!type.getName().equals("#UNKNOWN") && !type.getName().equals("int") && !type.isArray()) {
            reports.add(ReportUtils.incompatibleTypesReport(jmmNode, type.print(), "int"));
        }

        putType(jmmNode, new Type("int", true));

        return reports;
    }

    private List<Report> visitNot(JmmNode jmmNode, SymbolTable symbolTable) {
        List<Report> reports = new ArrayList<>();
        Type type = AnalysisUtils.getType(jmmNode.getJmmChild(0));

        if (type.getName().equals("#UKNOWN")) {
            putUnknownType(jmmNode);
            return reports;
        }

        if (type.isArray() || !type.getName().equals("boolean")) {
            putUnknownType(jmmNode);
            reports.add(ReportUtils.operatorCannotBeAppliedReport(jmmNode.getJmmChild(0), "!", type.print(), "boolean"));
            return reports;
        }

        putType(jmmNode, new Type("boolean", false));

        return reports;
    }

    private List<Report> visitBinaryOp(JmmNode jmmNode, SymbolTable symbolTable) {
        List<Report> reports = new ArrayList<>();
        Type lhsType = AnalysisUtils.getType(jmmNode.getJmmChild(0));
        Type rhsType = AnalysisUtils.getType(jmmNode.getJmmChild(1));

        if (lhsType.getName().equals("#UNKNOWN") || rhsType.getName().equals("#UNKNOWN")) {
            putUnknownType(jmmNode);
            return reports;
        }

        if (!lhsType.equals(rhsType)) {
            putUnknownType(jmmNode);
            reports.add(ReportUtils.incompatibleTypesReport(jmmNode, rhsType.print(), lhsType.print()));
            return reports;
        } else if (ARITHMETIC_OP.contains(jmmNode.get("op")) || LOGICAL_OP.contains(jmmNode.get("op"))) {
            if (lhsType.isArray()) {
                putUnknownType(jmmNode);
                reports.add(ReportUtils.operatorCannotBeAppliedReport(jmmNode, jmmNode.get("op"), lhsType.print(), rhsType.print()));
                return reports;
            }
        } else if (ARITHMETIC_OP.contains(jmmNode.get("op")) || COMPARISON_OP.contains(jmmNode.get("op"))) {
            if (!lhsType.getName().equals("int")) {
                putUnknownType(jmmNode);
                reports.add(ReportUtils.operatorCannotBeAppliedReport(jmmNode, jmmNode.get("op"), lhsType.print(), rhsType.print()));
                return reports;
            }
        } else if (LOGICAL_OP.contains(jmmNode.get("op"))) {
            if (!lhsType.getName().equals("boolean")) {
                putUnknownType(jmmNode);
                reports.add(ReportUtils.operatorCannotBeAppliedReport(jmmNode, jmmNode.get("op"), lhsType.print(), rhsType.print()));
                return reports;
            }
        } else if (!PRIMITIVES.contains(lhsType.getName())) {
            putUnknownType(jmmNode);
            reports.add(ReportUtils.operatorCannotBeAppliedReport(jmmNode, jmmNode.get("op"), lhsType.print(), rhsType.print()));
            return reports;
        }

        if (COMPARISON_OP.contains(jmmNode.get("op"))) {
            putType(jmmNode, new Type("boolean", false));
        } else {
            putType(jmmNode, lhsType);
        }

        return reports;
    }

    private List<Report> visitAssignment(JmmNode jmmNode, SymbolTable symbolTable) {
        List<Report> reports = new ArrayList<>();
        Type lhsType = AnalysisUtils.getType(jmmNode.getJmmChild(0));
        Type rhsType = AnalysisUtils.getType(jmmNode.getJmmChild(1));

        if (lhsType.getName().equals("#UNKNOWN") || rhsType.getName().equals("#UNKNOWN")) {
            return reports;
        }

        if (!lhsType.equals(rhsType)) {
            reports.add(ReportUtils.incompatibleTypesReport(jmmNode, rhsType.print(), lhsType.print()));
        }

        return reports;
    }

    private List<Report> visitCondition(JmmNode jmmNode, SymbolTable symbolTable) {
        List<Report> reports = new ArrayList<>();
        Type conditionType = AnalysisUtils.getType(jmmNode.getJmmChild(0));

        if (conditionType.getName().equals("#UNKNOWN")) {
            return reports;
        }

        if (conditionType.isArray() || !conditionType.getName().equals("boolean")) {
            reports.add(ReportUtils.incompatibleTypesReport(jmmNode.getJmmChild(0), conditionType.print(), "boolean"));
        }

        return reports;
    }

    private List<Report> visitReturn(JmmNode jmmNode, SymbolTable symbolTable) {
        List<Report> reports = new ArrayList<>();
        String methodSignature = jmmNode.getJmmParent().getJmmParent().get("signature");
        Type returnType = symbolTable.getReturnType(methodSignature);
        Type expressionType = AnalysisUtils.getType(jmmNode.getJmmChild(0));

        if (returnType.getName().equals("#UNKNOWN") || expressionType.getName().equals("#UNKNOWN")) {
            return reports;
        }

        if (!expressionType.equals(returnType)) {
            reports.add(ReportUtils.incompatibleTypesReport(jmmNode.getJmmChild(0), expressionType.print(), returnType.print()));
        }

        return reports;
    }

    private List<Report> visitType(JmmNode jmmNode, SymbolTable symbolTable) {
        List<Report> reports = new ArrayList<>();

        Type objType = AnalysisUtils.getType(jmmNode);
        if (PRIMITIVES.contains(objType.getName())) {
            return reports;
        }

        return checkTypeImport(jmmNode, symbolTable);
    }

    private List<Report> checkTypeImport(JmmNode jmmNode, SymbolTable symbolTable) {
        List<Report> reports = new ArrayList<>();

        Type objType = AnalysisUtils.getType(jmmNode);

        if (symbolTable.getClassName().equals(objType.getName()) || objType.getName().equals("String")) {
            return reports;
        }

        Optional<String> qualifiedName = symbolTable
                .getImports()
                .stream()
                .filter(imp -> imp.substring(imp.lastIndexOf(".") + 1).equals(objType.getName()))
                .findAny();

        if (qualifiedName.isPresent()) {
            return reports;
        }

        putUnknownType(jmmNode);
        reports.add(ReportUtils.cannotFindSymbolReport(jmmNode, objType.print()));

        return reports;
    }

    private void putType(JmmNode jmmNode, Type type) {
        jmmNode.put("type", type.getName());
        jmmNode.put("isArray", String.valueOf(type.isArray()));
    }

    private void putUnknownType(JmmNode jmmNode) {
        jmmNode.put("type", "#UNKNOWN");
    }
}

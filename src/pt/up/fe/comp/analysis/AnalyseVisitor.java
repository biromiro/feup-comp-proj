package pt.up.fe.comp.analysis;

import pt.up.fe.comp.jmm.analysis.table.SymbolTable;
import pt.up.fe.comp.jmm.analysis.table.Type;
import pt.up.fe.comp.jmm.ast.JmmNode;
import pt.up.fe.comp.jmm.ast.PostorderJmmVisitor;
import pt.up.fe.comp.jmm.report.Report;
import pt.up.fe.comp.jmm.report.ReportType;
import pt.up.fe.comp.jmm.report.Stage;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

// TODO better error messages and line/column numbers?
public class AnalyseVisitor extends PostorderJmmVisitor<SymbolTable, List<Report>> {
    static final List<String> PRIMITIVES = Arrays.asList("int", "void", "String", "boolean");
    static final List<String> ARITHMETIC_OP = Arrays.asList("ADD", "SUB", "MUL", "DIV");

    public AnalyseVisitor() {
        addVisit("Type", this::visitType);
        addVisit("ReturnStatement", this::visitReturn);
        addVisit("IfThenElseStatement", this::visitCondition);
        addVisit("WhileStatement", this::visitCondition);
        addVisit("Assignment", this::visitAssignment);
        addVisit("BinaryOp", this::visitBinaryOp);
    }

    private List<Report> visitBinaryOp(JmmNode jmmNode, SymbolTable symbolTable) {
        List<Report> reports = new ArrayList<>();
        Type lhsType = getType(jmmNode.getJmmChild(0));
        Type rhsType = getType(jmmNode.getJmmChild(1));

        if (!lhsType.equals(rhsType)) {
            reports.add(incompatibleTypesReport(rhsType.print(), lhsType.print()));
        } else if (ARITHMETIC_OP.contains(jmmNode.get("op"))) {
            if (lhsType.isArray() || rhsType.isArray()) {
                operatorCannotBeAppliedReport(jmmNode.get("op"), lhsType.print(), rhsType.print());
            }
        }

        return reports;
    }

    private List<Report> visitAssignment(JmmNode jmmNode, SymbolTable symbolTable) {
        List<Report> reports = new ArrayList<>();
        Type lhsType = getType(jmmNode.getJmmChild(0));
        Type rhsType = getType(jmmNode.getJmmChild(1));

        if (!lhsType.equals(rhsType)) {
            reports.add(incompatibleTypesReport(rhsType.print(), lhsType.print()));
        }

        return reports;
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
        if (PRIMITIVES.contains(jmmNode.get("type"))) {
            return reports;
        }

        // TODO check if type has been imported

        return reports;
    }

    private Type getType(JmmNode jmmNode) {
        JmmNode child = jmmNode.getJmmChild(0);
        boolean isArray = child.getAttributes().contains("isArray") && child.get("isArray").equals("true");
        return new Type(child.get("type"), isArray);
    }
}

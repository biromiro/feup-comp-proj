package pt.up.fe.comp.analysis;

import pt.up.fe.comp.jmm.analysis.table.Symbol;
import pt.up.fe.comp.jmm.analysis.table.Type;
import pt.up.fe.comp.jmm.ast.JmmNode;

import java.util.List;

public class AnalysisUtils {
    public static Type getType(JmmNode jmmNode) {
        boolean isArray = jmmNode.getAttributes().contains("isArray") && jmmNode.get("isArray").equals("true");
        return new Type(jmmNode.get("type"), isArray);
    }

    public static Symbol getSymbol(JmmNode jmmNode) {
        String name = jmmNode.get("name");
        Type type = getType(jmmNode.getJmmChild(0));
        return new Symbol(type, name);
    }

    public static String getMethodSignature(String methodName, List<Symbol> parameters) {
        StringBuilder methodSignatureBuilder = new StringBuilder();
        methodSignatureBuilder.append(methodName);
        for (Symbol parameter: parameters) {
            methodSignatureBuilder.append("#");
            methodSignatureBuilder.append(parameter.getType().print());
        }

        return methodSignatureBuilder.toString();
    }

    public static String getMethodSymbolName(String methodSignature) {
        if (methodSignature.contains("#")) {
            return methodSignature.replaceFirst("#", "(")
                    .replaceAll("##UNKNOWN", ",?")
                    .replaceAll("\\(#UNKNOWN", "(?")
                    .replaceAll("#", ",")
                    .concat(")");
        }
        return  methodSignature.concat("()");
    }
}

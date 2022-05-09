package pt.up.fe.comp.ollir;

import pt.up.fe.comp.jmm.analysis.table.Symbol;
import pt.up.fe.comp.jmm.analysis.table.Type;

public class OllirUtils {

    public static String getCode(Symbol symbol) {
        return "var_" + symbol.getName() + "." + getCode(symbol.getType());
    }

    public static String getCode(Symbol symbol, String index) {
        return "var_" + symbol.getName() + "[" + index + "]." + getCode(symbol.getType(), true);
    }

    public static String getCode(String value, Type type) {
        return value + "." + getCode(type);
    }

    public static String getTempCode(String value, Type type) {
        return "temp_" + value + "." + getCode(type);
    }

    public static String getCode(Type type, Boolean indexed) {
        StringBuilder code = new StringBuilder();

        if (type.isArray() && !indexed) {
            code.append("array.");
        }

        code.append(getOllirType(type.getName()));

        return code.toString();
    }

    public static String getCode(Type type) {
        return getCode(type, false);
    }

    public static String getOllirType(String jmmType) {

        switch (jmmType) {
            case "void":
            case "#UNKNOWN":
                return "V";
            case "int": return "i32";
            case "boolean": return "bool";
            default: return jmmType;
        }
    }
}

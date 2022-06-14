package pt.up.fe.comp.jasmin;

import org.specs.comp.ollir.*;
import pt.up.fe.specs.util.exceptions.NotImplementedException;

public class MyJasminUtils {
    public static String getFullyQualifiedName(ClassUnit context, String className) {
        for (String importString : context.getImports()) {
            var splitImports = importString.split("\\.");
            String lastName;
            if (splitImports.length == 0) {
                lastName = importString;
            } else {
                lastName = splitImports[splitImports.length - 1];
            }
            if (lastName.equals(className)) {
                return importString.replace('.', '/');
            }
        }
        return context.getClassName().replace("\\.", "/");
    }

    public static String getJasminType(ClassUnit context, Type type) {
        if (type instanceof ArrayType) {
            return "[" + getJasminType(((ArrayType)type).getArrayType());
        }
        if (type instanceof ClassType) {
            return "L" + getFullyQualifiedName(context, ((ClassType) type).getName()) + ";";
        }
        return getJasminType(type.getTypeOfElement());
    }

    public static String getJasminType(ElementType type) {
        return switch (type) {
            case INT32 -> "I";
            case STRING -> "Ljava/lang/String;";
            case VOID -> "V";
            case BOOLEAN -> "Z";
            default -> throw new NotImplementedException(type);
        };
    }
}

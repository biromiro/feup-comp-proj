package pt.up.fe.comp.jasmin;

import org.specs.comp.ollir.OperationType;

public class JasminInstruction {
    enum FieldInstruction {
        GET,
        PUT,
    }
    private static String registerInstruction(String inst, int register) {
        if (register >= 0 && register <= 3) {
            inst = inst + "_";
        } else {
            inst = inst + " ";
        }
        return inst + register + "\n";
    }

    public static String pop() {
        return "pop\n";
    }

    public static String dup() {
        return "dup\n";
    }

    public static String aload(int register) {
        return registerInstruction("aload", register);
    }

    public static String iload(int register) {
        return registerInstruction("iload", register);
    }

    public static String iaload() {
        return "iaload\n";
    }

    public static String astore(int register) {
        return registerInstruction("astore", register);
    }

    public static String istore(int register) {
        return registerInstruction("istore", register);
    }

    public static String iastore() {
        return "iastore\n";
    }

    public static String iconst(String num) {
        int integer = Integer.parseInt(num);
        String instruction = "";
        if (integer == -1) {
            instruction = "iconst_m1";
        }
        else if (integer >= 0 && integer <= 5) {
            instruction = "iconst_" + num;
        }
        else if (integer >= -128 && integer <= 127) {
            instruction = "bipush " + num;
        }
        else if (integer >= -32768 && integer <= 32767) {
            instruction = "sipush " + num;
        }
        else {
            instruction = "ldc " + num;
        }
        return instruction + "\n";
    }

    public static String arithmetic(OperationType type) {
        return switch (type) {
            case ADD -> "iadd\n";
            case SUB -> "isub\n";
            case MUL, ANDB -> "imul\n";
            case DIV -> "idiv\n";
            case OR -> "ior\n";
            default -> "";
        };
    }

    public static String arraylength() {
        return "arraylength\n";
    }

    public static String field(FieldInstruction type, String className, String fieldName, String fieldType) {
        return type.toString().toLowerCase() + "field" +
                " " + className +
                "/" + fieldName +
                " " + fieldType +
                "\n";
    }

    public static String new_(String className) {
        return "new " + className + '\n';
    }

    public static String newarray() {
        return "newarray int\n";
    }

    public static String goto_(String label) {
        return "goto " + label + '\n';
    }

    public static String ifne(String label) {
        return "ifne " + label + '\n';
    }
}

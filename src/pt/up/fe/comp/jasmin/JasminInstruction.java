package pt.up.fe.comp.jasmin;

import org.specs.comp.ollir.OperationType;

public class JasminInstruction {
    enum FieldInstruction {
        GET,
        PUT,
    }

    public static StackLimit stackLimit = new StackLimit();

    private static String registerInstruction(String inst, int register) {
        if (register >= 0 && register <= 3) {
            inst = inst + "_";
        } else {
            inst = inst + " ";
        }
        return inst + register + "\n";
    }

    public static String pop() {
        stackLimit.updateStack(-1);
        return "pop\n";
    }

    public static String dup() {
        stackLimit.updateStack(1);
        return "dup\n";
    }

    public static String aload(int register) {
        stackLimit.updateStack(1);
        return registerInstruction("aload", register);
    }

    public static String iload(int register) {
        stackLimit.updateStack(1);
        return registerInstruction("iload", register);
    }

    public static String iaload() {
        stackLimit.updateStack(-1);
        return "iaload\n";
    }

    public static String astore(int register) {
        stackLimit.updateStack(-1);
        return registerInstruction("astore", register);
    }

    public static String istore(int register) {
        stackLimit.updateStack(0);
        return registerInstruction("istore", register);
    }

    public static String iastore() {
        stackLimit.updateStack(-3);
        return "iastore\n";
    }

    public static String iconst(String num) {
        stackLimit.updateStack(1);
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
        stackLimit.updateStack(-1);
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
        stackLimit.updateStack(0);
        return "arraylength\n";
    }

    public static String field(FieldInstruction type, String className, String fieldName, String fieldType) {
        int stackDiff = type == FieldInstruction.GET ? 0 : -2;
        stackLimit.updateStack(stackDiff);
        return type.toString().toLowerCase() + "field" +
                " " + className +
                "/" + fieldName +
                " " + fieldType +
                "\n";
    }

    public static String new_(String className) {
        stackLimit.updateStack(1);
        return "new " + className + '\n';
    }

    public static String newarray() {
        stackLimit.updateStack(0);
        return "newarray int\n";
    }

    public static String goto_(String label) {
        stackLimit.updateStack(0);
        return "goto " + label + '\n';
    }

    public static String ifne(String label) {
        return "ifne " + label + '\n';
    }
}

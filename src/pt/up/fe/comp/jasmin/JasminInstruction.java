package pt.up.fe.comp.jasmin;

public class JasminInstruction {
    private static String registerInstruction(String inst, int register) {
        if (register >= 0 && register <= 3) {
            inst = inst + "_";
        } else {
            inst = inst + " ";
        }
        return inst + register + "\n";
    }

    public static String aload(int register) {
        return registerInstruction("aload", register);
    }

    public static String iload(int register) {
        return registerInstruction("iload", register);
    }

    public static String astore(int register) {
        return registerInstruction("astore", register);
    }

    public static String istore(int register) {
        return registerInstruction("istore", register);
    }

    public static String iconst(String num) {
        int integer = Integer.parseInt(num);
        String instruction = "";
        if (integer == -1) {
            instruction = "iconst_m1";
        } else if (integer >= 0 && integer <= 5) {
            instruction = "iconst_" + num;
        } else if (integer >= -128 && integer <= 127) {
            instruction = "bipush " + num;
        } else if (integer >= -32768 && integer <= 32767) {
            instruction = "sipush " + num;
        } else {
            instruction = "ldc " + num;
        }
        return instruction + "\n";
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

    public static String goto_(String label) {
        return "goto " + label + '\n';
    }

    public static String ifne(String label) {
        return "ifne " + label + '\n';
    }

    public static String iinc(int register, String literal) {
        return "iinc " + register + " " + literal + "\n";
    }

    public static String ifeq(String label) {
        return "ifeq " + label + '\n';
    }

    public static String iflt(String label) {
        return "iflt " + label + '\n';
    }

    public static String ifge(String label) {
        return "ifge " + label + '\n';
    }

    public static String if_icmplt(String label) {
        return "if_icmplt " + label + '\n';
    }

    public static String if_icmpge(String label) {
        return "if_icmpge " + label + '\n';
    }

    enum FieldInstruction {
        GET,
        PUT,
    }

}

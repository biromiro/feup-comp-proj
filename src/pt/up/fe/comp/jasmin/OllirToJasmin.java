package pt.up.fe.comp.jasmin;

import com.javacc.parser.tree.ReturnType;
import com.sun.jdi.IntegerType;
import org.specs.comp.ollir.*;
import pt.up.fe.comp.VOID;
import pt.up.fe.specs.util.SpecsIo;
import pt.up.fe.specs.util.classmap.FunctionClassMap;
import pt.up.fe.specs.util.exceptions.NotImplementedException;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Locale;
import java.util.stream.Collectors;

public class OllirToJasmin {

    private final ClassUnit classUnit;

    public OllirToJasmin(ClassUnit classUnit) {
        this.classUnit = classUnit;
        this.classUnit.buildVarTables();
    }

    public String getFullyQualifiedName(String className) {
        for (String importString : this.classUnit.getImports()) {
            var splittedImports = importString.split("\\.");
            String lastName;
            System.out.println("IMPORT: " + importString);
            if (splittedImports.length == 0) {
                lastName = importString;
            } else {
                lastName = splittedImports[splittedImports.length - 1];
            }
            System.out.println("lastName: " + lastName);
            System.out.println("ClassName: " + className);
            if (lastName.equals(className)) {
                return importString.replace('.', '/');
            }
        }
        throw new RuntimeException("Could not find import for class " + className);
    }

    public String getCode() {

        StringBuilder code = new StringBuilder();

        // Class name
        code.append(".class public ").append(this.classUnit.getClassName()).append("\n");
        String superClass = this.classUnit.getSuperClass();

        // Super class name
        String superClassName = superClass != null ? this.getFullyQualifiedName(superClass) : "java/lang/Object";
        code.append(".super ").append(superClassName).append("\n\n");

        // Super constructor
        code.append(SpecsIo.getResource("jasminConstructor.template").replace("${SUPER_NAME}", superClassName));

        code.append("\n\n\n");

        // Methods
        for (Method method : this.classUnit.getMethods()) {
            if (!method.isConstructMethod()) {
                code.append(this.getCode(method));

            }
        }

        return code.toString();
    }

    public String getCode(Method method) {
        StringBuilder code = new StringBuilder();

        // Modifiers
        AccessModifiers accessModifier = method.getMethodAccessModifier();
        String accessModifierName = "";
        if (accessModifier != AccessModifiers.DEFAULT) {
            accessModifierName = accessModifier.name().toLowerCase() + " ";
        }
        code.append(".method ").append(accessModifierName);
        if (method.isStaticMethod()) {
            code.append("static ");
        }

        // Method name
        code.append(method.getMethodName()).append("(");

        // Method parameters
        String methodParamTypes = method.getParams().stream()
                .map(element -> getJasminType(element.getType()))
                .collect(Collectors.joining());

        // Method return type
        code.append(methodParamTypes).append(")").append(getJasminType(method.getReturnType())).append("\n");

        code.append(".limit stack 99\n").append(".limit locals 99\n");

        // Method instructions
        for (Instruction instruction : method.getInstructions()) {
            System.out.println("instruction " + instruction.toString());
            code.append(getCode(method, instruction));
        }

        code.append(".end method\n");
        return code.toString();
    }

    public String getCode(Method method, Instruction instruction) {
        //FunctionClassMap<Instruction, String> instructionMap = new FunctionClassMap<>();
        //instructionMap.put(CallInstruction.class, this.getCode());

        if (instruction instanceof CallInstruction) {
            return getCode(method, (CallInstruction) instruction);
        }
        if (instruction instanceof ReturnInstruction) {
            return getCode(method, (ReturnInstruction) instruction);
        }
        if (instruction instanceof AssignInstruction) {
            return getCode(method, (AssignInstruction) instruction);
        }

        throw new NotImplementedException(instruction.getClass());
    }

    public String getCode(Method method, CallInstruction callInstruction) {
        var table = method.getVarTable();
        switch (callInstruction.getInvocationType()) {
            case invokestatic:
                return getCodeInvokeStatic(callInstruction);
            case invokespecial:
                return getCodeInvokeSpecial(callInstruction);
            case invokevirtual:
                return getCodeInvokeVirtual(callInstruction, table);
            default: {
                throw new NotImplementedException(callInstruction.getInvocationType());
                //return "";
            }
        }
    }

    public String getCode(Method method, AssignInstruction assignInstruction) {
        StringBuilder code = new StringBuilder();

        Element lhs = assignInstruction.getDest();
        Instruction rhs = assignInstruction.getRhs();
        var table = method.getVarTable();
        System.out.println("Here " + lhs.toString());
        System.out.println("Rhs: " + rhs.toString());
        String rhsString = getOperand(table, rhs);

        //store to lhs
        code.append(getStore(lhs, rhsString, method.getVarTable()));

        return code.toString();
    }

    private String getStore(Element lhs, String rhs, HashMap<String, Descriptor> table) {
        StringBuilder code = new StringBuilder();
        ElementType type = lhs.getType().getTypeOfElement();

        if (type == ElementType.INT32 || type == ElementType.STRING || type == ElementType.BOOLEAN) {
            int register = table.get(((Operand) lhs).getName()).getVirtualReg();
            return rhs + istore(register);
        }

        return "";
    }

    private String istore(int register) {
        String instruction = "istore";
        if (register >= 0 && register <= 3) {
            instruction = instruction + "_";
        }
        else {
            instruction = instruction + " ";
        }
        return instruction + register + "\n";
    }

    private String getOperand(HashMap<String, Descriptor> table, Instruction instruction) {
        switch (instruction.getInstType()) {
            case NOPER -> {
                return getNoper(table, (SingleOpInstruction) instruction);
            }
            case BINARYOPER -> {
                return getBinaryOper(table, (BinaryOpInstruction) instruction);
            }
            default -> {
                return "\n";
            }
        }
    }

    private String getBinaryOper(HashMap<String, Descriptor> table, BinaryOpInstruction instruction) {
        StringBuilder code = new StringBuilder();
        Element lhs = instruction.getLeftOperand();
        Element rhs = instruction.getRightOperand();

        String leftLoad = getLoad(table, lhs);
        String rightLoad = getLoad(table, rhs);
        OperationType type = instruction.getOperation().getOpType();

        //grammar accepts &&, ! and <
        boolean isBooleanOp = type == OperationType.ANDB || type == OperationType.NOTB || type == OperationType.LTH;

        if (!isBooleanOp) {
            code.append(leftLoad);
            code.append(rightLoad);
            code.append(getArithmetic(type));
        }
        return code.toString();
    }

    private String getArithmetic(OperationType type) {
        return switch (type) {
            case ADD -> "iadd\n";
            case SUB -> "isub\n";
            case MUL -> "imul\n";
            case DIV -> "idiv\n";
            default -> "";
        };
    }

    private String getNoper(HashMap<String, Descriptor> table, SingleOpInstruction instruction) {
        Element element = instruction.getSingleOperand();
        return getLoad(table, element);
    }

    private String getLoad(HashMap<String, Descriptor> table, Element element) {
        ElementType type = element.getType().getTypeOfElement();

        // instruction of the iconst family
        if (element.isLiteral()) {
            System.out.println("hit iconst");
            return iconst(((LiteralElement) element).getLiteral());
        }

        // instruction of the iload family
        if (type == ElementType.INT32 || type == ElementType.STRING || type == ElementType.BOOLEAN) {
            System.out.println("hit iload");
            int register = table.get(((Operand) element).getName()).getVirtualReg();
            return iload(register);
        }

        // instruction of the aload family
        if (type == ElementType.OBJECTREF || type == ElementType.ARRAYREF || type == ElementType.THIS) {
            System.out.println("hit aload");
            int register = table.get(((Operand) element).getName()).getVirtualReg();
            return aload(register);
        }
        System.out.println("Here, type was " + type.name());
        return "";
    }

    private String aload(int register) {
        String instruction = "aload";
        if (register >= 0 && register <= 3) {
            instruction = instruction + "_";
        } else {
            instruction = instruction + " ";
        }
        return instruction + register + "\n";
    }

    private String iload(int register) {
        String instruction = "iload";
        if (register >= 0 && register <= 3) {
            instruction = instruction + "_";
        }
        else {
            instruction = instruction + " ";
        }
        return instruction + register + "\n";
    }

    private String iconst(String num) {
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

    public String getCodeInvokeStatic(CallInstruction instruction) {
        StringBuilder code = new StringBuilder();

        code.append("invokestatic ");
        System.out.println();
        String methodClass = ((Operand) instruction.getFirstArg()).getName();
        code.append(getFullyQualifiedName(methodClass));
        code.append("/");
        code.append(((LiteralElement) instruction.getSecondArg()).getLiteral().replace("\"", ""));
        code.append(getArgumentsCode(instruction.getListOfOperands()));
        code.append(getJasminType(instruction.getReturnType()));
        code.append("\n");

        return code.toString();
    }

    public String getCodeInvokeSpecial(CallInstruction instruction) {
        StringBuilder code = new StringBuilder();
        code.append("invokespecial java/lang/Object/<init>()V ; call super\n");
        return code.toString();
    }

    public String getCodeInvokeVirtual(CallInstruction instruction, HashMap<String, Descriptor> table) {
        StringBuilder code = new StringBuilder();
        ArrayList<Element> parameters = instruction.getListOfOperands();
        String methodName = ((LiteralElement) instruction.getSecondArg()).getLiteral().replace("\"", "");
        String className = ((ClassType) instruction.getFirstArg().getType()).getName();
        Type returnType = instruction.getReturnType();

        code.append(getLoad(table, instruction.getFirstArg()));
        for (Element parameter : parameters) {
            code.append(getLoad(table, parameter));
        }

        code.append("invokevirtual ").append(className).append("/");
        code.append(methodName.replace("\"", ""));
        code.append(getArgumentsCode(parameters));
        code.append(getJasminType(returnType));
        code.append("\n");
        System.out.println("now " + code.toString());
        System.out.println("parameters: " + parameters);


        return code.toString();
    }

    public String getCode(Method method, ReturnInstruction returnInstruction) {
        if(!returnInstruction.hasReturnValue())
            return "return\n";

        StringBuilder code = new StringBuilder();

        Element result = returnInstruction.getOperand();
        code.append(getLoad(method.getVarTable(), result));
        ElementType type = result.getType().getTypeOfElement();
        if(type == ElementType.INT32 || type == ElementType.BOOLEAN) {
            code.append("ireturn\n");
        }
        else {
            code.append("areturn\n");
        }

        return code.toString();
    }

    private String getArgumentsCode(ArrayList<Element> operands) {
        StringBuilder code = new StringBuilder();
        code.append("(");
        for (Element argument : operands) {
            code.append(getJasminType(argument.getType()));
        }
        code.append(")");
        return code.toString();
    }

    public String getJasminType(Type type) {
        if (type instanceof ArrayType) {
            return "[" + getJasminType(((ArrayType)type).getTypeOfElements());
        }
        return getJasminType(type.getTypeOfElement());
    }

    public String getJasminType(ElementType type) {
        switch (type) {
            case INT32 -> {
                return "I";
            }
            case STRING -> {
                return "Ljava/lang/String;";
            }
            case VOID -> {
                return "V";
            }
            case BOOLEAN -> {
                return "Z";
            }
            default -> {
                throw new NotImplementedException(type);
            }
        }
        //return "";
    }


}

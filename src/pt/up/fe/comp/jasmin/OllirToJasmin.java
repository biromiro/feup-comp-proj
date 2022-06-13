package pt.up.fe.comp.jasmin;

import org.specs.comp.ollir.*;
import pt.up.fe.specs.util.exceptions.NotImplementedException;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.stream.Collectors;

public class OllirToJasmin {

    private final ClassUnit classUnit;
    private int comparisons = 0;

    public OllirToJasmin(ClassUnit classUnit) {
        this.classUnit = classUnit;
        this.classUnit.buildVarTables();
    }

    public String getFullyQualifiedName(String className) {
        for (String importString : this.classUnit.getImports()) {
            var splittedImports = importString.split("\\.");
            String lastName;
            if (splittedImports.length == 0) {
                lastName = importString;
            } else {
                lastName = splittedImports[splittedImports.length - 1];
            }
            if (lastName.equals(className)) {
                return importString.replace('.', '/');
            }
        }
        return this.classUnit.getClassName().replace("\\.", "/");
    }

    public String getCode() {

        StringBuilder code = new StringBuilder();

        // Class name
        code.append(".class public ").append(this.classUnit.getClassName()).append("\n");
        String superClass = this.classUnit.getSuperClass();

        // Super class name
        String superClassName = superClass != null ? this.getFullyQualifiedName(superClass) : "java/lang/Object";
        code.append(".super ").append(superClassName).append("\n\n");

        for (Field field : this.classUnit.getFields()) {
            code.append(this.getCode(field));
        }

        // Super constructor
        code.append(".method public <init>()V\n")
                .append("aload_0\n")
                .append("invokespecial ").append(superClassName).append("/<init>()V\n")
                .append("return\n")
                .append(".end method\n\n");

        code.append("\n\n\n");

        // Methods
        for (Method method : this.classUnit.getMethods()) {
            if (!method.isConstructMethod()) {
                code.append(this.getCode(method));

            }
        }

        return code.toString();
    }

    public String getCode(Field field) {
        StringBuilder code = new StringBuilder();

        // Modifiers
        AccessModifiers accessModifier = field.getFieldAccessModifier();
        StringBuilder accessModifierName = new StringBuilder();
        if (accessModifier != AccessModifiers.DEFAULT) {
            accessModifierName.append(accessModifier.name().toLowerCase()).append(" ");
        }
        if (field.isStaticField()) {
            accessModifierName.append("static ");
        }
        if (field.isFinalField()) {
            accessModifierName.append("final ");
        }
        code.append(".field ").append(accessModifierName);


        // Field name
        code.append(field.getFieldName()).append(" ");

        // Field return type
        code.append(getJasminType(field.getFieldType()));

        // Initialization
        if (field.isInitialized()) {
            code.append(" = ").append(field.getInitialValue());
        }

        code.append("\n");

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

        HashMap<String, Instruction> labels = method.getLabels();

        // Method instructions
        for (Instruction instruction : method.getInstructions()) {
            for (String label : labels.keySet()) {
                if (labels.get(label) == instruction) {
                    code.append(label + ":\n");
                }
            }
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
        if (instruction instanceof GetFieldInstruction) {
            return getCode(method, (GetFieldInstruction) instruction);
        }
        if (instruction instanceof PutFieldInstruction) {
            return getCode(method, (PutFieldInstruction) instruction);
        }
        if (instruction instanceof CondBranchInstruction) {
            return getCode(method, (CondBranchInstruction) instruction);
        }
        if (instruction instanceof GotoInstruction) {
            return getCode(method, (GotoInstruction) instruction);
        }

        throw new NotImplementedException(instruction.getClass());
    }

    public String getCode(Method method, GotoInstruction instruction) {
        return "goto " + instruction.getLabel() + "\n";
    }

    public String getCode(Method method, CondBranchInstruction instruction) {
        StringBuilder code = new StringBuilder();
        Element condition = ((SingleOpCondInstruction) instruction).getCondition().getSingleOperand();
        /*code.append(getNoper(method.getVarTable(), ((SingleOpCondInstruction) instruction).getCondition()));
        if (condition.isLiteral()) {
            code.append("ifne ").append(instruction.getLabel());
        } else {
            code.append("if_icmplt ").append(instruction.getLabel());
        }*/
        //code.append(getNoper(method.getVarTable(), ((SingleOpCondInstruction) instruction).getCondition()));
        code.append(getLoad(method.getVarTable(), condition));
        code.append("ifne ").append(instruction.getLabel());
        code.append("\n");
        return code.toString();
    }

    public String getCode(Method method, GetFieldInstruction fieldInstruction) {
        StringBuilder code = new StringBuilder();
        Element classElement = fieldInstruction.getFirstOperand();
        Element fieldElement = fieldInstruction.getSecondOperand();
        code.append(getLoad(method.getVarTable(), classElement));
        code.append(getField(classElement, fieldElement));
        return code.toString();
    }

    private String getField(Element classElement, Element fieldElement) {
        StringBuilder code = new StringBuilder();
        String fieldName = ((Operand) fieldElement).getName();
        String className = getFullyQualifiedName(((ClassType) classElement.getType()).getName());
        code.append("getfield ").append(className).append("/")
                .append(fieldName).append(" ").append(getJasminType(fieldElement.getType())).append("\n");
        return code.toString();
    }

    private String putField(Element classElement, Element fieldElement) {
        StringBuilder code = new StringBuilder();
        String fieldName = ((Operand) fieldElement).getName();
        String className = getFullyQualifiedName(((ClassType) classElement.getType()).getName());
        code.append("putfield ").append(className).append("/")
                .append(fieldName).append(" ").append(getJasminType(fieldElement.getType())).append("\n");
        return code.toString();
    }

    public String getCode(Method method, PutFieldInstruction fieldInstruction) {
        StringBuilder code = new StringBuilder();
        Element classElement = fieldInstruction.getFirstOperand();
        Element fieldElement = fieldInstruction.getSecondOperand();
        Element valueElement = fieldInstruction.getThirdOperand();
        code.append(getLoad(method.getVarTable(), classElement));
        code.append(getLoad(method.getVarTable(), valueElement));
        code.append(putField(classElement, fieldElement));
        return code.toString();
    }

    public String getCode(Method method, CallInstruction callInstruction) {
        switch (callInstruction.getInvocationType()) {
            case arraylength:
                return getCodeArrayLength(callInstruction, method);
            case NEW:
                return getCodeNew(callInstruction, method);
            case invokestatic:
                return getCodeInvokeStatic(callInstruction, method);
            case invokespecial:
                return getCodeInvokeSpecial(callInstruction, method);
            case invokevirtual:
                return getCodeInvokeVirtual(callInstruction, method);
            default: {
                throw new NotImplementedException(callInstruction.getInvocationType());
            }
        }
    }

    private String getCodeArrayLength(CallInstruction callInstruction, Method method) {
        StringBuilder code = new StringBuilder();
        code.append(getLoad(method.getVarTable(), callInstruction.getFirstArg()));
        code.append("arraylength\n");
        return code.toString();
    }

    public String getCodeNew(CallInstruction callInstruction, Method method) {
        callInstruction.show();
        StringBuilder code = new StringBuilder();
        ElementType type = callInstruction.getReturnType().getTypeOfElement();
        if (type != ElementType.ARRAYREF) {
            String returnType = ((ClassType) callInstruction.getReturnType()).getName();
            code.append(getCodeNewObject(returnType));
        } else {
            String load = getLoad(method.getVarTable(), callInstruction.getListOfOperands().get(0));
            code.append(getCodeNewArray(load));
        }
        return code.toString();
    }

    private String getCodeNewObject(String typeName) {
        return newCall(typeName) + "dup\n";
    }

    private String getCodeNewArray(String load) {
        return load + "newarray int\n";
    }

    private String newCall(String className) {
        return "new " + getFullyQualifiedName(className) + '\n';
    }

    public String getCode(Method method, AssignInstruction assignInstruction) {
        StringBuilder code = new StringBuilder();
        Element lhs = assignInstruction.getDest();
        Instruction rhs = assignInstruction.getRhs();
        String rhsString = getOperand(method, rhs);

        //store to lhs
        String val = getStore(lhs, rhsString, method.getVarTable());
        code.append(val);

        return code.toString();
    }

    private String getStore(Element lhs, String rhs, HashMap<String, Descriptor> table) {
        StringBuilder code = new StringBuilder();
        ElementType type = lhs.getType().getTypeOfElement();
        int register = table.get(((Operand) lhs).getName()).getVirtualReg();

        if (type == ElementType.INT32 ||
                type == ElementType.STRING ||
                type == ElementType.BOOLEAN) {
            ElementType variableType = table.get(((Operand) lhs).getName()).getVarType().getTypeOfElement();
            if (variableType == ElementType.ARRAYREF) {
                return getStoreArray(lhs, rhs, table);
            }
            return rhs + istore(register);
        } else if (type == ElementType.OBJECTREF ||
                type == ElementType.THIS ||
                type == ElementType.ARRAYREF) {
            return rhs + astore(register);
        }

        return "";
    }

    private String getStoreArray(Element element, String rhs, HashMap<String, Descriptor> table) {
        int arrayRegister = table.get(((Operand) element).getName()).getVirtualReg();
        int indexRegister = table.get(((Operand) ((ArrayOperand) element).getIndexOperands().get(0)).getName()).getVirtualReg();
        return aload(arrayRegister) + iload(indexRegister) + rhs + "iastore\n";
    }

    private String astore(int register) {
        String instruction = "astore";
        if (register >= 0 && register <= 3) {
            instruction = instruction + "_";
        } else {
            instruction = instruction + " ";
        }
        return instruction + register + "\n";
    }

    private String istore(int register) {
        String instruction = "istore";
        if (register >= 0 && register <= 3) {
            instruction = instruction + "_";
        } else {
            instruction = instruction + " ";
        }
        return instruction + register + "\n";
    }

    private String getOperand(Method method, Instruction instruction) {
        switch (instruction.getInstType()) {
            case NOPER -> {
                return getNoper(method.getVarTable(), (SingleOpInstruction) instruction);
            }
            case UNARYOPER -> {
                return getUnaryOper(method.getVarTable(), (UnaryOpInstruction) instruction);
            }
            case BINARYOPER -> {
                return getBinaryOper(method.getVarTable(), (BinaryOpInstruction) instruction);
            }
            case CALL -> {
                return getCode(method, (CallInstruction) instruction);
            }
            case GETFIELD -> {
                return getCode(method, (GetFieldInstruction) instruction);
            }
            default -> {
                return "\n";
            }
        }
    }

    private String getUnaryOper(HashMap<String, Descriptor> table, UnaryOpInstruction instruction) {
        StringBuilder code = new StringBuilder();
        Element element = instruction.getOperand();
        Operation operation = instruction.getOperation();

        if (operation.getOpType() == OperationType.NOTB) {
            code.append(iconst("1"));
            code.append(getLoad(table, element));
            code.append("isub\n");
        }

        return code.toString();
    }

    private String getBinaryOper(HashMap<String, Descriptor> table, BinaryOpInstruction instruction) {
        StringBuilder code = new StringBuilder();
        Element lhs = instruction.getLeftOperand();
        Element rhs = instruction.getRightOperand();

        String leftLoad = getLoad(table, lhs);
        String rightLoad = getLoad(table, rhs);
        OperationType type = instruction.getOperation().getOpType();

        if (type == OperationType.LTH || type == OperationType.ANDB) {
            code.append(getBoolean(type, leftLoad, rightLoad));
        } else {
            code.append(leftLoad);
            code.append(rightLoad);
            code.append(getArithmetic(type));
        }
        return code.toString();
    }

    private String getBoolean(OperationType type, String leftLoad, String rightLoad) {
        return switch (type) {
            case LTH -> getLth(leftLoad, rightLoad);
            case ANDB -> getAndB(leftLoad, rightLoad);
            default -> "";
        };
    }

    private int nextLabelNumber() {
        return this.comparisons++;
    }

    private String getLth(String leftLoad, String rightLoad) {
        StringBuilder code = new StringBuilder();
        code.append(leftLoad);
        code.append(rightLoad);
        String label1 = "LTH_" + nextLabelNumber();
        String label2 = "LTH_" + nextLabelNumber();
        code.append("if_icmplt ").append(label1).append("\n")
                .append(iconst("0")).append("goto ")
                .append(label2).append("\n").append(label1).append(":\n")
                .append(iconst("1")).append(label2).append(":\n");
        return code.toString();
    }

    private String getAndB(String leftLoad, String rightLoad) {
        StringBuilder code = new StringBuilder();
        String label1 = "ANDB_" + nextLabelNumber();
        String label2 = "ANDB_" + nextLabelNumber();

        // if any side is 0, then result is false
        code.append(leftLoad);
        code.append("ifeq ").append(label1).append("\n");

        code.append(rightLoad);
        code.append("ifeq ").append(label1).append("\n");

        // result is 1
        code.append(iconst("1"));
        code.append("goto ").append(label2).append("\n");

        // result is 0
        code.append(label1).append(":\n");
        code.append(iconst("0"));

        code.append(label2).append(":\n");

        return code.toString();
    }


    private String getArithmetic(OperationType type) {
        return switch (type) {
            case ADD -> "iadd\n";
            case SUB -> "isub\n";
            case MUL, ANDB -> "imul\n";
            case DIV -> "idiv\n";
            case OR -> "ior\n";
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
            return iconst(((LiteralElement) element).getLiteral());
        }

        // instruction of the iload family
        if (type == ElementType.INT32 || type == ElementType.STRING || type == ElementType.BOOLEAN) {
            ElementType variableType = table.get(((Operand) element).getName()).getVarType().getTypeOfElement();
            if (variableType == ElementType.ARRAYREF) {
                return getLoadArray(element, table);
            }
            int register = table.get(((Operand) element).getName()).getVirtualReg();
            return iload(register);
        }

        // instruction of the aload family
        if (type == ElementType.OBJECTREF || type == ElementType.ARRAYREF || type == ElementType.THIS) {
            int register = table.get(((Operand) element).getName()).getVirtualReg();
            return aload(register);
        }

        return "";
    }

    private String getLoadArray(Element element, HashMap<String, Descriptor> table) {
        int arrayRegister = table.get(((Operand) element).getName()).getVirtualReg();
        int indexRegister = table.get(((Operand) ((ArrayOperand) element).getIndexOperands().get(0)).getName()).getVirtualReg();
        return aload(arrayRegister) + iload(indexRegister) + "iaload\n";
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
        } else {
            instruction = instruction + " ";
        }
        return instruction + register + "\n";
    }

    private String iconst(String num) {
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

    public String getCodeInvokeStatic(CallInstruction instruction, Method method) {
        HashMap<String, Descriptor> table = method.getVarTable();

        StringBuilder code = new StringBuilder();
        ArrayList<Element> parameters = instruction.getListOfOperands();

        for (Element parameter : parameters) {
            code.append(getLoad(table, parameter));
        }

        code.append("invokestatic ");

        String methodClass = ((Operand) instruction.getFirstArg()).getName();
        code.append(getFullyQualifiedName(methodClass));
        code.append("/");
        code.append(((LiteralElement) instruction.getSecondArg()).getLiteral().replace("\"", ""));
        code.append(getArgumentsCode(instruction.getListOfOperands()));
        code.append(getJasminType(instruction.getReturnType()));
        code.append("\n");

        return code.toString();
    }

    public String getCodeInvokeSpecial(CallInstruction instruction, Method method) {

        HashMap<String, Descriptor> table = method.getVarTable();
        StringBuilder code = new StringBuilder();
        ArrayList<Element> parameters = instruction.getListOfOperands();
        String methodName = ((LiteralElement) instruction.getSecondArg()).getLiteral().replace("\"", "");
        String className = getFullyQualifiedName(((ClassType) instruction.getFirstArg().getType()).getName());
        Type returnType = instruction.getReturnType();

        for (Element parameter : parameters) {
            code.append(getLoad(table, parameter));
        }

        code.append(getLoad(table, instruction.getFirstArg()));

        code.append("invokespecial ").append(className).append("/");
        code.append(methodName.replace("\"", ""));
        code.append(getArgumentsCode(parameters));
        code.append(getJasminType(returnType));
        code.append("\n");

        return code.toString();
    }

    public String getCodeInvokeVirtual(CallInstruction instruction, Method method) {
        HashMap<String, Descriptor> table = method.getVarTable();
        StringBuilder code = new StringBuilder();
        ArrayList<Element> parameters = instruction.getListOfOperands();
        String methodName = ((LiteralElement) instruction.getSecondArg()).getLiteral().replace("\"", "");
        String className = getFullyQualifiedName(((ClassType) instruction.getFirstArg().getType()).getName());
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


        return code.toString();
    }

    public String getCode(Method method, ReturnInstruction returnInstruction) {
        if (!returnInstruction.hasReturnValue())
            return "return\n";

        StringBuilder code = new StringBuilder();

        Element result = returnInstruction.getOperand();
        code.append(getLoad(method.getVarTable(), result));
        ElementType type = result.getType().getTypeOfElement();
        if (type == ElementType.INT32 || type == ElementType.BOOLEAN) {
            code.append("ireturn\n");
        } else {
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
            return "[" + getJasminType(((ArrayType) type).getTypeOfElements());
        }
        if (type instanceof ClassType) {
            return "L" + getFullyQualifiedName(((ClassType) type).getName()) + ";";
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

package pt.up.fe.comp.jasmin;

import org.specs.comp.ollir.*;
import pt.up.fe.specs.util.exceptions.NotImplementedException;

import java.util.ArrayList;

public class InstructionBuilder {
    private final Method method;

    private final LabelTracker labelTracker;

    public InstructionBuilder(Method method, LabelTracker labelTracker){
        this.method = method;
        this.labelTracker = labelTracker;
    }

    public String build(Instruction instruction) {
        return switch (instruction.getInstType()) {
            case CALL -> build((CallInstruction) instruction);
            case RETURN -> build((ReturnInstruction) instruction);
            case ASSIGN -> build((AssignInstruction) instruction);
            case GETFIELD -> build((GetFieldInstruction) instruction);
            case PUTFIELD -> build((PutFieldInstruction) instruction);
            case NOPER -> build((SingleOpInstruction) instruction);
            case UNARYOPER -> build((UnaryOpInstruction) instruction);
            case BINARYOPER -> build((BinaryOpInstruction) instruction);
            case GOTO -> build((GotoInstruction) instruction);
            case BRANCH -> build((CondBranchInstruction) instruction);
        };
    }

    private int registerNum(Element element) {
        return lookup(element).getVirtualReg();
    }

    private String getFullyQualifiedName(String className) {
        return MyJasminUtils.getFullyQualifiedName(this.method.getOllirClass(), className);
    }

    private String getJasminType(Type type) {
        return MyJasminUtils.getJasminType(this.method.getOllirClass(), type);
    }

    private String getArray(Element element) {
        int arrayRegister = registerNum(element);
        Element indexOperand = ((ArrayOperand) element).getIndexOperands().get(0);
        int indexRegister = registerNum(indexOperand);
        return JasminInstruction.aload(arrayRegister) + JasminInstruction.iload(indexRegister);
    }

    private String loadArray(Element element) {
        return getArray(element) + JasminInstruction.iaload();
    }

    private String storeArray(Element element, String rhs) {
        return getArray(element) + rhs + JasminInstruction.iastore();
    }

    private String loadParameters(ArrayList<Element> parameters) {
        StringBuilder code = new StringBuilder();
        for (Element parameter : parameters) {
            code.append(load(parameter));
        }
        return code.toString();
    }

    private String argumentTypes(ArrayList<Element> parameters) {
        StringBuilder code = new StringBuilder();
        code.append("(");
        for (Element argument : parameters) {
            code.append(getJasminType(argument.getType()));
        }
        code.append(")");
        return code.toString();
    }

    private Descriptor lookup(Element element) {
        return method.getVarTable().get(((Operand) element).getName());
    }

    private String load(Element element) {
        ElementType type = element.getType().getTypeOfElement();

        // instruction of the iconst family
        if (element.isLiteral()) {
            return JasminInstruction.iconst(((LiteralElement) element).getLiteral());
        }

        // instruction of the iload family
        if (type == ElementType.INT32 || type == ElementType.STRING || type == ElementType.BOOLEAN) {
            int register = registerNum(element);
            ElementType variableType = lookup(element).getVarType().getTypeOfElement();
            if (variableType == ElementType.ARRAYREF) {
                return loadArray(element);
            }
            return JasminInstruction.iload(register);
        }

        // instruction of the aload family
        if (type == ElementType.OBJECTREF || type == ElementType.ARRAYREF || type == ElementType.THIS) {
            int register = registerNum(element);
            return JasminInstruction.aload(register);
        }

        throw new NotImplementedException(type);
    }

    private String store(Element lhs, String rhs) {
        ElementType type = lhs.getType().getTypeOfElement();

        if (type == ElementType.INT32 || type == ElementType.STRING || type == ElementType.BOOLEAN) {
            ElementType variableType = lookup(lhs).getVarType().getTypeOfElement();
            if (variableType == ElementType.ARRAYREF) {
                return storeArray(lhs, rhs);
            }
            return rhs + JasminInstruction.istore(registerNum(lhs));
        } else if (type == ElementType.OBJECTREF || type == ElementType.THIS || type == ElementType.ARRAYREF) {
            return rhs + JasminInstruction.astore(registerNum(lhs));
        }

        return "";
    }

    private String arraylength(CallInstruction instruction) {
        return load(instruction.getFirstArg()) + JasminInstruction.arraylength();
    }

    private String newCall(CallInstruction instruction) {
        StringBuilder code = new StringBuilder();
        ElementType type = instruction.getReturnType().getTypeOfElement();
        if (type != ElementType.ARRAYREF) {
            String returnType = ((ClassType) instruction.getReturnType()).getName();
            code.append(newObject(returnType));
        } else {
            code.append(load(instruction.getListOfOperands().get(0)));
            code.append(JasminInstruction.newarray());
        }
        return code.toString();
    }

    private String newObject(String className) {
        return JasminInstruction.new_(getFullyQualifiedName(className)) + JasminInstruction.dup();
    }

    private String invokestatic(CallInstruction instruction) {
        StringBuilder code = new StringBuilder();
        ArrayList<Element> parameters = instruction.getListOfOperands();
        code.append(loadParameters(parameters));
        code.append("invokestatic ");

        String methodClass = getFullyQualifiedName(((Operand) instruction.getFirstArg()).getName());
        String methodName = ((LiteralElement) instruction.getSecondArg()).getLiteral().replace("\"", "");
        Type returnType = instruction.getReturnType();

        code.append(methodClass).append("/")
                .append(methodName)
                .append(argumentTypes(parameters))
                .append(getJasminType(returnType))
                .append("\n");

        return code.toString();
    }

    private String invokenonstatic(CallInstruction instruction) {
        StringBuilder code = new StringBuilder();
        ArrayList<Element> parameters = instruction.getListOfOperands();
        code.append(load(instruction.getFirstArg()));
        code.append(loadParameters(parameters));

        String className = getFullyQualifiedName(((ClassType) instruction.getFirstArg().getType()).getName());
        String methodName = ((LiteralElement) instruction.getSecondArg()).getLiteral().replace("\"", "");
        Type returnType = instruction.getReturnType();

        code.append(instruction.getInvocationType().toString()).append(" ")
                .append(className).append("/")
                .append(methodName)
                .append(argumentTypes(parameters))
                .append(getJasminType(returnType))
                .append("\n");

        return code.toString();
    }

    private String fieldOp(Element classElement, Element fieldElement, InstructionType instructionType) {
        JasminInstruction.FieldInstruction fieldOp = instructionType.equals(InstructionType.GETFIELD) ?
                JasminInstruction.FieldInstruction.GET : JasminInstruction.FieldInstruction.PUT;
        String className = getFullyQualifiedName(((ClassType) classElement.getType()).getName());
        String fieldName = ((Operand) fieldElement).getName();
        String fieldType = getJasminType(fieldElement.getType());
        return JasminInstruction.field(fieldOp, className, fieldName, fieldType);
    }

    private String build(CallInstruction instruction) {
        return switch (instruction.getInvocationType()) {
            case arraylength -> arraylength(instruction);
            case NEW -> newCall(instruction);
            case invokestatic -> invokestatic(instruction);
            case invokespecial, invokevirtual -> invokenonstatic(instruction);
            default -> throw new NotImplementedException(instruction.getInvocationType());
        };
    }

    private String build(ReturnInstruction instruction) {
        if(!instruction.hasReturnValue())
            return "return\n";
        StringBuilder code = new StringBuilder();
        Element result = instruction.getOperand();
        code.append(load(result));
        ElementType type = result.getType().getTypeOfElement();
        if(type == ElementType.INT32 || type == ElementType.BOOLEAN) {
            code.append("ireturn\n");
        }
        else {
            code.append("areturn\n");
        }
        return code.toString();
    }

    private String build(AssignInstruction instruction) {
        StringBuilder code = new StringBuilder();
        Element lhs = instruction.getDest();
        Instruction rhs = instruction.getRhs();
        String rhsString = build(rhs);

        //store to lhs
        String val = store(lhs, rhsString);
        code.append(val);

        return code.toString();
    }

    private String build(GetFieldInstruction instruction) {
        StringBuilder code = new StringBuilder();
        Element classElement = instruction.getFirstOperand();
        code.append(load(classElement));
        Element fieldElement = instruction.getSecondOperand();
        code.append(fieldOp(classElement, fieldElement, InstructionType.GETFIELD));
        return code.toString();
    }

    private String build(PutFieldInstruction instruction) {
        StringBuilder code = new StringBuilder();
        Element classElement = instruction.getFirstOperand();
        Element fieldElement = instruction.getSecondOperand();
        Element valueElement = instruction.getThirdOperand();
        code.append(load(classElement));
        code.append(load(valueElement));
        code.append(fieldOp(classElement, fieldElement, InstructionType.PUTFIELD));
        return code.toString();
    }

    private String build(SingleOpInstruction instruction) {
        Element element = instruction.getSingleOperand();
        return load(element);
    }

    private String build(UnaryOpInstruction instruction) {
        StringBuilder code = new StringBuilder();
        Element element = instruction.getOperand();
        Operation operation = instruction.getOperation();

        if (operation.getOpType() == OperationType.NOTB) {
            code.append(JasminInstruction.iconst("1"));
            code.append(load(element));
            code.append(JasminInstruction.arithmetic(OperationType.SUB));
        }

        return code.toString();
    }

    private String build(BinaryOpInstruction instruction) {
        StringBuilder code = new StringBuilder();
        Element lhs = instruction.getLeftOperand();
        Element rhs = instruction.getRightOperand();

        String leftLoad = load(lhs);
        String rightLoad = load(rhs);
        OperationType type = instruction.getOperation().getOpType();

        //grammar also accepts && and <

        if (type == OperationType.LTH || type == OperationType.ANDB) {
            code.append(booleanBinary(type, leftLoad, rightLoad));
        } else {
            code.append(leftLoad);
            code.append(rightLoad);
            code.append(JasminInstruction.arithmetic(type));
        }

        return code.toString();
    }

    private String booleanBinary(OperationType type, String leftLoad, String rightLoad) {
        return switch (type) {
            case LTH -> lth(leftLoad, rightLoad);
            case ANDB -> andb(leftLoad, rightLoad);
            default -> "";
        };
    }

    private String lth(String leftLoad, String rightLoad) {
        StringBuilder code = new StringBuilder();
        code.append(leftLoad);
        code.append(rightLoad);
        String label1 = "LTH_" + labelTracker.nextLabelNumber();
        String label2 = "LTH_" + labelTracker.nextLabelNumber();
        code.append("if_icmplt ").append(label1).append("\n")
                .append(JasminInstruction.iconst("0")).append("goto ")
                .append(label2).append("\n").append(label1).append(":\n")
                .append(JasminInstruction.iconst("1")).append(label2).append(":\n");
        return code.toString();
    }

    private String andb(String leftLoad, String rightLoad) {
        StringBuilder code = new StringBuilder();
        String label1 = "ANDB_" + labelTracker.nextLabelNumber();
        String label2 = "ANDB_" + labelTracker.nextLabelNumber();

        // if any side is 0, then result is false
        code.append(leftLoad);
        code.append("ifeq ").append(label1).append("\n");

        code.append(rightLoad);
        code.append("ifeq ").append(label1).append("\n");

        // result is 1
        code.append(JasminInstruction.iconst("1"));
        code.append("goto ").append(label2).append("\n");

        // result is 0
        code.append(label1).append(":\n");
        code.append(JasminInstruction.iconst("0"));

        code.append(label2).append(":\n");

        return code.toString();
    }


    private String build(GotoInstruction instruction) {
        return JasminInstruction.goto_(instruction.getLabel());
    }

    private String build(CondBranchInstruction instruction) {
        StringBuilder code = new StringBuilder();
        Element condition = ((SingleOpCondInstruction) instruction).getCondition().getSingleOperand();
        code.append(load(condition));
        code.append(JasminInstruction.ifne(instruction.getLabel()));
        code.append("\n");
        return code.toString();
    }
}

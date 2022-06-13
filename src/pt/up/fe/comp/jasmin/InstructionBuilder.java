package pt.up.fe.comp.jasmin;

import org.specs.comp.ollir.*;
import pt.up.fe.specs.util.exceptions.NotImplementedException;

import java.util.ArrayList;

public class InstructionBuilder {
    private final Method method;

    public InstructionBuilder(Method method) {
        this.method = method;
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
            default -> throw new NotImplementedException(instruction.getClass());
        };
    }

    private int registerNum(Element element) {
        var table = method.getVarTable();
        String elementName = ((Operand) element).getName();
        return table.get(elementName).getVirtualReg();
    }

    private String getFullyQualifiedName(String className) {
        return MyJasminUtils.getFullyQualifiedName(this.method.getOllirClass(), className);
    }

    private String getJasminType(Type type) {
        return MyJasminUtils.getJasminType(this.method.getOllirClass(), type);
    }

    private String load(Element element) {
        ElementType type = element.getType().getTypeOfElement();

        // instruction of the iconst family
        if (element.isLiteral()) {
            return JasminInstruction.iconst(((LiteralElement) element).getLiteral());
        }

        // instruction of the iload family
        if (type == ElementType.INT32 || type == ElementType.STRING || type == ElementType.BOOLEAN) {
            int register = method.getVarTable().get(((Operand) element).getName()).getVirtualReg();
            return JasminInstruction.iload(register);
        }

        // instruction of the aload family
        if (type == ElementType.OBJECTREF || type == ElementType.ARRAYREF || type == ElementType.THIS) {
            int register = method.getVarTable().get(((Operand) element).getName()).getVirtualReg();
            return JasminInstruction.aload(register);
        }

        throw new NotImplementedException(type);
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

    private String store(Element lhs) {
        ElementType type = lhs.getType().getTypeOfElement();
        int register = registerNum(lhs);

        if (type == ElementType.INT32 || type == ElementType.STRING || type == ElementType.BOOLEAN) {
            return JasminInstruction.istore(register);
        } else if (type == ElementType.OBJECTREF || type == ElementType.THIS || type == ElementType.ARRAYREF) {
            return JasminInstruction.astore(register);
        }   else throw new NotImplementedException(type);
    }

    private String arithmetic(OperationType type) {
        return switch (type) {
            case ADD -> "iadd\n";
            case SUB -> "isub\n";
            case MUL, ANDB -> "imul\n";
            case DIV -> "idiv\n";
            case OR -> "ior\n";
            default -> "";
        };
    }

    private String newCall(CallInstruction instruction) {
        StringBuilder code = new StringBuilder();
        String returnType = ((ClassType)instruction.getReturnType()).getName();
        String returnClass = getFullyQualifiedName(returnType);
        code.append(JasminInstruction._new(returnClass));
        code.append("dup\n");
        return code.toString();
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
        code.append(loadParameters(parameters));
        code.append(load(instruction.getFirstArg()));

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
        code.append(build(rhs));

        //store to lhs
        String val = store(lhs);
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
        code.append(load(fieldElement));
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
            code.append("isub\n");
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

        code.append(leftLoad);
        code.append(rightLoad);
        code.append(arithmetic(type));

        return code.toString();
    }
}

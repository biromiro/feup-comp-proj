package pt.up.fe.comp.jasmin;

import org.specs.comp.ollir.*;
import pt.up.fe.specs.util.SpecsIo;
import pt.up.fe.specs.util.classmap.FunctionClassMap;
import pt.up.fe.specs.util.exceptions.NotImplementedException;

import java.util.Locale;
import java.util.stream.Collectors;

public class OllirToJasmin {

    private final ClassUnit classUnit;

    public OllirToJasmin(ClassUnit classUnit) {
        this.classUnit = classUnit;
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
            code.append(this.getCode(method));
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
        var methodParamTypes = method.getParams().stream()
                .map(element -> getJasminType(element.getType()))
                .collect(Collectors.joining());

        // Method return type
        code.append(methodParamTypes).append(")").append(getJasminType(method.getReturnType())).append("\n");

        code.append(".limit stack 99\n").append(".limit locals 99\n");

        // Method instructions
        for (Instruction instruction : method.getInstructions()) {
            code.append(getCode(instruction));
        }

        code.append(".end method\n");
        return code.toString();
    }

    public String getCode(Instruction instruction) {
        //FunctionClassMap<Instruction, String> instructionMap = new FunctionClassMap<>();
        //instructionMap.put(CallInstruction.class, this.getCode());

        if (instruction instanceof CallInstruction) {
            return getCode((CallInstruction)instruction);
        }

        throw new NotImplementedException(instruction.getClass());
    }

    public String getCode(CallInstruction callInstruction) {

        switch (callInstruction.getInvocationType()) {
            case invokestatic:
                return getCodeInvokeStatic(callInstruction);
            default: {
                throw new NotImplementedException(callInstruction.getInvocationType());
            }
        }

    }

    public String getCodeInvokeStatic(CallInstruction instruction) {
        StringBuilder code = new StringBuilder();

        code.append("invokestatic ");

        var methodClass = ((Operand) instruction.getFirstArg()).getName();
        code.append(getFullyQualifiedName(methodClass));
        code.append("/");
        code.append(((LiteralElement) instruction.getSecondArg()).getLiteral());
        code.append("(");
        for (var operand : instruction.getListOfOperands()) {
            getArgumentCode(operand);
        }
        code.append(")");
        code.append(getJasminType(instruction.getReturnType()));

        return code.toString();
    }

    private void getArgumentCode(Element operand) {
        throw new NotImplementedException(this);
    }

    public String getJasminType(Type type) {
        if (type instanceof ArrayType) {
            return "[" + getJasminType(((ArrayType)type).getTypeOfElements());
        }
        return getJasminType(type.getTypeOfElement());
    }

    public String getJasminType(ElementType type) {
        switch (type) {
            case INT32: {
                return "I";
            }
            case STRING: {
                return "Ljava/lang/String;";
            }
            case VOID: {
                return "V";
            }
            case BOOLEAN: {
                return "Z";
            }
            default: {
                throw new NotImplementedException(type);
            }
        }
        //return "";
    }


}

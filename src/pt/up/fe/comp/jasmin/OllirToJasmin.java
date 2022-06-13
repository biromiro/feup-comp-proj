package pt.up.fe.comp.jasmin;

import org.specs.comp.ollir.*;

import java.util.stream.Collectors;

public class OllirToJasmin {

    private final ClassUnit classUnit;

    public OllirToJasmin(ClassUnit classUnit) {
        this.classUnit = classUnit;
        this.classUnit.buildVarTables();
    }

    public String build() {

        StringBuilder code = new StringBuilder();

        // Class name
        code.append(".class public ")
                .append(classUnit.getClassName())
                .append("\n");

        // Super class name
        String superClass = classUnit.getSuperClass();
        String superClassName = superClass != null ? MyJasminUtils.getFullyQualifiedName(classUnit, superClass) : "java/lang/Object";
        code.append(".super ")
                .append(superClassName)
                .append("\n\n");

        // Field declarations
        for (Field field : classUnit.getFields()) {
            code.append(build(field));
        }

        // Super constructor
        code.append(".method public <init>()V\n")
                .append("aload_0\n")
                .append("invokespecial ").append(superClassName).append("/<init>()V\n")
                .append("return\n")
                .append(".end method\n\n");

        code.append("\n\n\n");

        // Methods
        for (Method method : classUnit.getMethods()) {
            if (!method.isConstructMethod()) {
                code.append(build(method));
            }
        }

        return code.toString();
    }

    public String build(Field field) {
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
        code.append(MyJasminUtils.getJasminType(classUnit, field.getFieldType()));

        // Initialization
        if (field.isInitialized()){
            code.append(" = ").append(field.getInitialValue());
        }

        code.append("\n");

        return code.toString();
    }

    public String build(Method method) {
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
                .map(element -> MyJasminUtils.getJasminType(classUnit, element.getType()))
                .collect(Collectors.joining());

        // Method return type
        code.append(methodParamTypes).append(")").append(MyJasminUtils.getJasminType(classUnit, method.getReturnType())).append("\n");

        code.append(".limit stack 99\n").append(".limit locals 99\n");

        InstructionBuilder codeBuilder = new InstructionBuilder(method);

        // Method instructions
        for (Instruction instruction : method.getInstructions()) {
            code.append(codeBuilder.build(instruction));
        }

        code.append(".end method\n");
        return code.toString();
    }
}

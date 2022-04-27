package pt.up.fe.comp.analysis;

import pt.up.fe.comp.jmm.analysis.table.Symbol;
import pt.up.fe.comp.jmm.analysis.table.SymbolTable;
import pt.up.fe.comp.jmm.analysis.table.Type;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ConcreteSymbolTable implements SymbolTable {

    private final Map<String, MethodInfo> methods = new HashMap<>();
    private List<String> imports = new ArrayList<>();
    private String className = null;
    private String superClass = null;
    private List<Symbol> fields = new ArrayList<>();

    @Override
    public List<String> getImports() {
        return imports;
    }

    public void addImport(String importPath) {
        imports.add(importPath);
    }

    @Override
    public String getClassName() {
        return className;
    }

    public void setClassName(String name) {
        className = name;
    }

    @Override
    public String getSuper() {
        return superClass;
    }

    public void setSuper(String name) {
        superClass = name;
    }

    @Override
    public List<Symbol> getFields() {
        return fields;
    }

    public void addField(Symbol field) {
        fields.add(field);
    }

    @Override
    public List<String> getMethods() {
        return new ArrayList<>(methods.keySet());
    }

    public void addMethod(String methodName, Type returnType, List<Symbol> parameters, List<Symbol> variables) {
        methods.put(getMethodSignature(methodName, parameters), new MethodInfo(returnType, parameters, variables));
    }

    public static String getMethodSignature(String methodName, List<Symbol> parameters) {
        StringBuilder methodSignatureBuilder = new StringBuilder();
        methodSignatureBuilder.append(methodName);
        for (Symbol parameter: parameters) {
            methodSignatureBuilder.append("#");
            methodSignatureBuilder.append(getTypeSignature(parameter.getType()));
        }

        return methodSignatureBuilder.toString();
    }

    private static String getTypeSignature(Type type) {
        StringBuilder builder = new StringBuilder();
        builder.append(type.getName());
        if (type.isArray()) {
            builder.append("[]");
        }

        return builder.toString();
    }

    @Override
    public Type getReturnType(String methodSignature) {
        return methods.get(methodSignature).getReturnType();
    }

    @Override
    public List<Symbol> getParameters(String methodSignature) {
        return methods.get(methodSignature).getParameters();
    }

    @Override
    public List<Symbol> getLocalVariables(String methodSignature) {
        return methods.get(methodSignature).getLocalVariables();
    }

}

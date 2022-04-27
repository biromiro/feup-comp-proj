package pt.up.fe.comp;

import pt.up.fe.comp.jmm.analysis.table.Symbol;
import pt.up.fe.comp.jmm.analysis.table.Type;

import java.util.List;

public class MethodInfo {

    private Type returnType;
    private List<Symbol> parameters;
    private List<Symbol> variables;

    public MethodInfo(Type returnType, List<Symbol> parameters, List<Symbol> variables) {
        this.returnType = returnType;
        this.parameters = parameters;
        this.variables = variables;
    }

    public Type getReturnType() {
        return returnType;
    }

    public List<Symbol> getParameters() {
        return parameters;
    }

    public List<Symbol> getLocalVariables() {
        return variables;
    }
}

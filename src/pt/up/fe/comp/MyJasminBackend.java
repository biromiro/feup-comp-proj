package pt.up.fe.comp;

import org.specs.comp.ollir.ClassUnit;
import org.specs.comp.ollir.Method;
import pt.up.fe.comp.jmm.jasmin.JasminBackend;
import pt.up.fe.comp.jmm.jasmin.JasminResult;
import pt.up.fe.comp.jmm.ollir.OllirResult;

import java.util.ArrayList;

public class MyJasminBackend implements JasminBackend {
    @Override
    public JasminResult toJasmin(OllirResult ollirResult) {
        ClassUnit ollir = ollirResult.getOllirClass();

        return null;
    }

    private String genMethods(ClassUnit ollir) {
        ArrayList<Method> methods = ollir.getMethods();

        StringBuilder code = new StringBuilder();

        for (Method method : methods) {

        }

        return null;
    }
}

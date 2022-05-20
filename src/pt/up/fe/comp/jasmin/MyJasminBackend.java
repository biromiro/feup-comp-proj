package pt.up.fe.comp.jasmin;

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
        String jasminCode = new OllirToJasmin(ollir).getCode();

        System.out.println("JASMIN CODE:\n" + jasminCode);
        return new JasminResult(jasminCode);
    }
}
package pt.up.fe.comp.jasmin;

import org.specs.comp.ollir.ClassUnit;
import pt.up.fe.comp.jmm.jasmin.JasminBackend;
import pt.up.fe.comp.jmm.jasmin.JasminResult;
import pt.up.fe.comp.jmm.ollir.OllirResult;

public class MyJasminBackend implements JasminBackend {
    @Override
    public JasminResult toJasmin(OllirResult ollirResult) {
        ClassUnit ollir = ollirResult.getOllirClass();
        String jasminCode = new OllirToJasmin(ollir).build();

        if (ollirResult.getConfig().getOrDefault("debug", "true").equals("true")) {
            System.out.println("JASMIN CODE:");
            System.out.println(jasminCode);
        }

        return new JasminResult(jasminCode);
    }
}

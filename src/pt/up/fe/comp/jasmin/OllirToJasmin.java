package pt.up.fe.comp.jasmin;

import org.specs.comp.ollir.ClassUnit;

public class OllirToJasmin {

    private final ClassUnit classUnit;

    public OllirToJasmin(ClassUnit classUnit) {
        this.classUnit = classUnit;
    }

    public String getFullyQualifiedName(String className) {
        for (String importString : this.classUnit.getImports()) {
            var splittedImports = importString.split("\\.");
            var lastName = splittedImports[splittedImports.length - 1];

            if (splittedImports.length == 0) {
                lastName = importString;
            }

            if (lastName.equals(className)) {
                return importString.replace('.', '/');
            }
        }
        throw new RuntimeException("Could not find import for class " + className);
    }

    public String getCode() {

        StringBuilder code = new StringBuilder();
        code.append(".class public ").append(this.classUnit.getClassName()).append("\n");
        code.append(".super ").append(this.getFullyQualifiedName(classUnit.getSuperClass())).append("\n");

        return code.toString();
    }


}

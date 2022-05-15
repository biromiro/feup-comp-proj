package pt.up.fe.comp.ollir;

public class Option {
    private TypeDecl type;
    private String context;

    public Option(TypeDecl type) {
        this.type = type;
        this.context = "";
    }

    public Option() {
        this.type = TypeDecl.UNDEFINED;
        this.context = "";
    }
    public Option(TypeDecl type, String context) {
        this.type = type;
        this.context = context;
    }

    public TypeDecl getType() {
        return type;
    }

    public String getContext() {
        return context;
    }
}

package pt.up.fe.comp.parse;

import pt.up.fe.comp.BaseNode;
import pt.up.fe.comp.jmm.ast.JmmNode;
import pt.up.fe.comp.jmm.ast.PreorderJmmVisitor;

public class LineColAnnotator extends PreorderJmmVisitor<Void, Void> {
    public LineColAnnotator() {
        setDefaultVisit(this::anotateLineCol);
    }

    private Void anotateLineCol(JmmNode jmmNode, Void unused) {
        // TODO col is not 100% correct for example on methods because PUBLIC is not considered
        BaseNode baseNode = (BaseNode) jmmNode;
        jmmNode.put("line", String.valueOf(baseNode.getBeginLine()));
        jmmNode.put("col", String.valueOf(baseNode.getBeginColumn()));
        return null;
    }

}

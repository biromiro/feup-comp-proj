package pt.up.fe.comp.ollir.optimize;

import java.util.HashSet;
import java.util.Set;

public class InterferenceGraph {

    private final Set<RegisterNode> nodes;

    public InterferenceGraph(Set<String> nodes) {
        this.nodes = new HashSet<>();
        for (String node: nodes) {
            this.nodes.add(new RegisterNode(node));
        }
    }

    public void addEdge(RegisterNode r1, RegisterNode r2) {
        r1.addEdge(r2);
        r2.addEdge(r1);
    }

    public void removeEdge(RegisterNode r1, RegisterNode r2) {
        r1.removeEdge(r2);
        r2.removeEdge(r1);
    }

    public Set<RegisterNode> getNodes() {
        return nodes;
    }

    public int getVisibleNodesCount() {
        int count = 0;
        for (RegisterNode node: nodes) {
            if (node.isVisible()) count++;
        }
        return count;
    }
}

package pt.up.fe.comp.ollir.optimize;

import org.specs.comp.ollir.*;
import pt.up.fe.comp.jmm.ollir.OllirResult;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Set;

public class MethodDataFlowAnalysis {

    private final Method method;
    private ArrayList<Set<String>> def;
    private ArrayList<Set<String>> use;
    private ArrayList<Set<String>> in;
    private ArrayList<Set<String>> out;
    private ArrayList<Node> nodeOrder;

    public MethodDataFlowAnalysis(Method method) {
        this.method = method;
        orderNodes();
    }

    private void orderNodes() {
        Node beginNode = method.getBeginNode();
        this.nodeOrder = new ArrayList<>();
        dfsOrderNodes(beginNode);
    }

    private void dfsOrderNodes(Node node) {

        if (node == null || nodeOrder.contains(node)) {
            return;
        }

        dfsOrderNodes(node.getSucc1());
        dfsOrderNodes(node.getSucc2());

        nodeOrder.add(node);
    }

    public void calcInOut() {
        System.out.println("-------------------------");
        in = new ArrayList<>();
        out = new ArrayList<>();
        def = new ArrayList<>();
        use = new ArrayList<>();

        for (Node node: nodeOrder) {
            in.add(new HashSet<>());
            out.add(new HashSet<>());
            def.add(new HashSet<>());
            use.add(new HashSet<>());
            calcUseDef(node);
            System.out.println("nodeuse" + use.get(nodeOrder.indexOf(node)));
            System.out.println("nodedef" + def.get(nodeOrder.indexOf(node)));
        }

        boolean livenessHasChanged;

        do  {
            livenessHasChanged = false;

            for (int index = 0; index < nodeOrder.size(); index++) {
                Node node = nodeOrder.get(index);

                // out[n] = (union (for all s that belongs to succ[n])) in[s]
                // in[n] = use[n] union (out[n] - def[n])

                Set<String> origIn = new HashSet<>(in.get(index));
                Set<String> origOut = new HashSet<>(out.get(index));

                out.get(index).clear();

                for (Node succ : node.getSuccessors()) {
                    int succIndex = nodeOrder.indexOf(succ);
                    Set<String> in_succIndex = in.get(succIndex);

                    out.get(index).addAll(in_succIndex);
                }

                in.get(index).clear();

                Set<String> outDefDiff = new HashSet<>(out.get(index));
                outDefDiff.removeAll(def.get(index));

                outDefDiff.addAll(use.get(index));
                in.get(index).addAll(outDefDiff);

                livenessHasChanged = livenessHasChanged ||
                        !origIn.equals(in.get(index)) || !origOut.equals(out.get(index));
                System.out.println(livenessHasChanged);
                System.out.println("node " + node + " in: " + in.get(index) + " out: " + out.get(index));
            }

        } while (livenessHasChanged);
    }

    private void addToUseDefSet(Node node, Element val, ArrayList<Set<String>> arr) {
        int index = nodeOrder.indexOf(node);

        if (val instanceof ArrayOperand arrop) {
            for (Element element: arrop.getIndexOperands()) {
                setUse(node, element);
            }
        }
        if (val instanceof Operand op) {
            arr.get(index).add(op.getName());
        }
    }


    private void setDef(Node node, Element dest) {
        addToUseDefSet(node, dest, def);
    }


    private void setUse(Node node, Element val) {
        addToUseDefSet(node, val, use);
    }

    private void calcUseDef(Node node) {
        calcUseDef(node, null);
    }

    private void calcUseDef(Node node, Node parentNode) {

        if (node == null) return;

        Node useDefNode = parentNode == null ? node : parentNode;

        if (node.getNodeType().equals(NodeType.BEGIN)) {
            return;
        }

        if (node.getNodeType().equals(NodeType.END)) {
            return;
        }

        if (node instanceof AssignInstruction instruction) {
            setDef(useDefNode, instruction.getDest());
            calcUseDef(instruction.getRhs(), node);
        } else if (node instanceof UnaryOpInstruction instruction) {
            setUse(useDefNode, instruction.getOperand());
        } else if (node instanceof BinaryOpInstruction instruction) {
            setUse(useDefNode, instruction.getLeftOperand());
            setUse(useDefNode, instruction.getRightOperand());
        } else if (node instanceof ReturnInstruction instruction) {
            setUse(useDefNode, instruction.getOperand());
        } else if (node instanceof CallInstruction instruction) {
            setUse(useDefNode, instruction.getFirstArg());
            for (Element arg: instruction.getListOfOperands()) {
                setUse(useDefNode, arg);
            }
        } else if (node instanceof GetFieldInstruction instruction) {
            setUse(useDefNode, instruction.getFirstOperand());
        } else if (node instanceof PutFieldInstruction instruction) {
            setUse(useDefNode, instruction.getFirstOperand());
            setUse(useDefNode, instruction.getThirdOperand());
        } else if (node instanceof SingleOpInstruction instruction) {
            setUse(useDefNode, instruction.getSingleOperand());
        } else if (node instanceof OpCondInstruction instruction) {
            for (Element operand: instruction.getOperands()) {
                setUse(useDefNode, operand);
            }
        } else if (node instanceof SingleOpCondInstruction instruction) {
            for (Element operand: instruction.getOperands()) {
                setUse(useDefNode, operand);
            }
        }
    }
}

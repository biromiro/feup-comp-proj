package pt.up.fe.comp.ollir.optimize;

import org.specs.comp.ollir.*;
import pt.up.fe.comp.jmm.ollir.OllirResult;
import pt.up.fe.comp.jmm.report.Report;
import pt.up.fe.comp.jmm.report.ReportType;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Set;
import java.util.Stack;

import static pt.up.fe.comp.jmm.report.Stage.OPTIMIZATION;

public class MethodDataFlowAnalysis {

    private final Method method;
    private final OllirResult ollirResult;
    private ArrayList<Set<String>> def;
    private ArrayList<Set<String>> use;
    private ArrayList<Set<String>> in;
    private ArrayList<Set<String>> out;
    private ArrayList<Node> nodeOrder;

    private InterferenceGraph interferenceGraph;

    public MethodDataFlowAnalysis(Method method, OllirResult ollirResult) {
        this.method = method;
        this.ollirResult = ollirResult;
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
            }

        } while (livenessHasChanged);
    }

    private void addToUseDefSet(Node node, Element val, ArrayList<Set<String>> arr) {
        int index = nodeOrder.indexOf(node);

        if (val instanceof ArrayOperand arrop) {
            for (Element element: arrop.getIndexOperands()) {
                setUse(node, element);
            }
            arr.get(index).add(arrop.getName());
        }

        if (val instanceof Operand op && !op.getType().getTypeOfElement().equals(ElementType.THIS)) {
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
            if (instruction.getListOfOperands() != null) {
                for (Element arg: instruction.getListOfOperands()) {
                    setUse(useDefNode, arg);
                }
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

    public void buildInterferenceGraph() {
        interferenceGraph = new InterferenceGraph(method.getVarTable().keySet());

        for (RegisterNode varX: interferenceGraph.getNodes()) {
            for (RegisterNode varY: interferenceGraph.getNodes()) {
                if (varX.equals(varY)) {
                    continue;
                }
                for (int index = 0; index < nodeOrder.size(); index++) {
                    Node node = nodeOrder.get(index);
                    if (def.get(index).contains(varX.getName())
                            && out.get(index).contains(varY.getName())) {
                        // TODO point 4 in https://cse.sc.edu/~mgv/csce531sp20/notes/mogensen_Ch8_Slides_register-allocation.pdf
                        interferenceGraph.addEdge(varX, varY);
                    }
                }
            }
        }
    }

    public void colorInterferenceGraph(int MAX_K) {
        Stack<RegisterNode> stack = new Stack<>();
        int k = 0;
        while (interferenceGraph.getVisibleNodesCount() > 0) {
            for (RegisterNode node: interferenceGraph.getNodes()) {
                if (!node.isVisible()) continue;
                int degree = node.countVisibleNeighbors();
                if (degree < k) {
                    node.setInvisible();
                    stack.push(node);
                } else {
                    k += 1;
                }
            }
        }

        if (MAX_K > 0 && k > MAX_K) {
            ollirResult.getReports().add(
                    new Report(
                            ReportType.ERROR,
                            OPTIMIZATION,
                            -1,
                            "Not enough registers. At least " + k + " registers are needed.")
            );
            throw new RuntimeException("Not enough registers." +
                    " At least " + k + " registers are needed but " + MAX_K + " were requested.");

        }

        while (!stack.empty()) {
            RegisterNode node = stack.pop();
            for (int reg = 1; reg <= k; reg++) {
                if (node.edgeFreeRegister(reg)) {
                    node.setRegister(reg);
                    node.setVisible();
                    break;
                }
            }
            if (!node.isVisible()) {
                ollirResult.getReports().add(
                        new Report(
                                ReportType.ERROR,
                                OPTIMIZATION,
                                -1,
                                "Unexpected error. Register allocation failed.")
                );

                throw new RuntimeException("Unexpected error. Register allocation failed.");
            }
        }
    }

    public InterferenceGraph getInterferenceGraph() {
        return interferenceGraph;
    }

    public Method getMethod() {
        return method;
    }

    public void eliminateDeadVars() {
    }

}

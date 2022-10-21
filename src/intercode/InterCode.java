package intercode;

import intercode.Operand.VirtualReg;
import util.NodeList;
import util.NodeListNode;

import java.io.IOException;
import java.util.function.Consumer;

public class InterCode {
    private final NodeList<Quaternion> list = new NodeList<>();
    private int regIdx = 1;
    private int labelIdx = 1;

    public void output(String filename) throws IOException {
        ResultOutput.output(this, filename);
    }

    public VirtualReg newReg() {
        return new VirtualReg(regIdx++);
    }

    public Label newLabel() {
        return new Label("label_" + labelIdx++);
    }

    // delegations
    public void addFirst(Quaternion q) {
        list.addFirst(q);
    }

    public void addLast(Quaternion q) {
        list.addLast(q);
    }

    public void forEach(Consumer<NodeListNode<Quaternion>> func) {
        list.forEach(func);

    }
}

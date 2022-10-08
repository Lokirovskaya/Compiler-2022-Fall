package intercode;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.Map;

import static intercode.Operand.*;

public class InterCode {
    private final Map<Integer, Integer> virtualRegMap = new HashMap<>();
    public final LinkedList<Quaternion> list = new LinkedList<>();

    public void output(String filename) {
        ResultOutput.output(this, filename);
    }

    void addQuater(Quaternion quater) {
        if (quater.target != null)
            virtualRegMap.putIfAbsent(quater.target.regID, null);
        if (quater.x1 instanceof VirtualReg)
            virtualRegMap.putIfAbsent(((VirtualReg) quater.x1).regID, null);
        if (quater.x2 instanceof VirtualReg)
            virtualRegMap.putIfAbsent(((VirtualReg) quater.x2).regID, null);
        list.addLast(quater);
    }

    public Integer getRegValue(int regID) {
        return virtualRegMap.get(regID);
    }

    public void setRegValue(int regID, int value) {
        virtualRegMap.replace(regID, value);
    }

    public void optimize() {
        optimizer.ReduceConst.run(this);
        optimizer.AssignRegister.run(this);
    }
}

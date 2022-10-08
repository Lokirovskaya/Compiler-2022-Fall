package intercode;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.Map;

import static intercode.Operand.*;

public class InterCode {
    private final Map<Integer, Integer> virtualRegMap = new HashMap<>();
    public final LinkedList<Quaternion> list = new LinkedList<>();

    void addQuaternion(Quaternion quater) {
        if (quater.target != null)
            virtualRegMap.put(quater.target.reg, null);
        if (quater.x1 instanceof VirtualReg)
            virtualRegMap.put(((VirtualReg) quater.x1).reg, null);
        if (quater.x2 instanceof VirtualReg)
            virtualRegMap.put(((VirtualReg) quater.x2).reg, null);
        list.addLast(quater);
    }

    public Integer getRegValue(int regCode) {
        return virtualRegMap.get(regCode);
    }

    public void setRegValue(int regCode, int value) {
        virtualRegMap.replace(regCode, value);
    }

    public void optimize() {
        optimizer.ReduceConst.run(this);
        optimizer.AssignRegister.run(this);
    }
}

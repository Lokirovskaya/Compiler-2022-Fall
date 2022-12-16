package optimizer.misc;

import intercode.Operand.VirtualReg;
import intercode.Quaternion;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static intercode.Quaternion.OperatorType.*;

public class FoldTempVar {
    public static void run(List<Quaternion> inter) {
        Set<VirtualReg> tempVarSet = findTempVars(inter);
        // temp = a op b
        // c = temp
        // 化为：c = a op b
        for (int i = 0; i < inter.size() - 1; i++) {
            Quaternion now = inter.get(i);
            Quaternion next = inter.get(i + 1);
            if (now.target != null && now.op != SET_ARRAY && tempVarSet.contains(now.target)) {
                if (next.op == SET && next.x1.equals(now.target)) {
                    now.target = next.target;
                    inter.remove(i + 1);
                }
            }
        }
    }

    private static Set<VirtualReg> findTempVars(List<Quaternion> inter) {
        Set<VirtualReg> tempVarSet = new HashSet<>();
        Set<VirtualReg> notTempVarSet = new HashSet<>();
        for (Quaternion q : inter) {
            for (VirtualReg vreg : q.getUseVregList()) {
                if (!notTempVarSet.contains(vreg)) {
                    if (!tempVarSet.contains(vreg)) {
                        tempVarSet.add(vreg);
                    }
                    else {
                        tempVarSet.remove(vreg);
                        notTempVarSet.add(vreg);
                    }
                }
            }
        }
        return tempVarSet;
    }
}

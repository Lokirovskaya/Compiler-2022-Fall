package optimizer;

import intercode.Operand.VirtualReg;
import intercode.Quaternion;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class ClearUnusedVar {
    static void run(List<Quaternion> inter) {
        Set<VirtualReg> vregRef = new HashSet<>();
        for (Quaternion q : inter) {
            vregRef.addAll(q.getUseVregList());
        }
        inter.removeIf(q -> q.target != null && !vregRef.contains(q.target));
    }
}

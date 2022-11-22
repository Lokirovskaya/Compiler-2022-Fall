package optimizer.misc;

import intercode.Operand.VirtualReg;
import intercode.Quaternion;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class ClearUnusedVar {
    public static void run(List<Quaternion> inter) {
        Set<VirtualReg> vregRef = new HashSet<>();
        for (Quaternion q : inter) {
            vregRef.addAll(q.getUseVregList());
            if (q.op == Quaternion.OperatorType.GETINT)
                vregRef.add(q.target);
        }
        inter.removeIf(q -> q.target != null && !vregRef.contains(q.target));
    }
}

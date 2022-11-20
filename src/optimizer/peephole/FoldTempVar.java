package optimizer.peephole;

import intercode.Operand;
import intercode.Operand.VirtualReg;
import intercode.Quaternion;

import java.util.List;

import static intercode.Quaternion.OperatorType.*;

public class FoldTempVar {
    // @t = ...
    // @x = @t
    // 优化为：@x = ...
    public static void run(List<Quaternion> inter) {
        for (int i = 0; i < inter.size() - 1; i++) {
            VirtualReg tVreg = null, xVreg = null;
            if (inter.get(i).op != SET_ARRAY && inter.get(i).target != null && inter.get(i).target.isTemp)
                tVreg = inter.get(i).target;
            if (tVreg == null) continue;

            if (inter.get(i + 1).op == SET && inter.get(i + 1).x1 instanceof VirtualReg && inter.get(i + 1).x1 == tVreg)
                xVreg = inter.get(i + 1).target;
            if (xVreg == null) continue;

            inter.get(i).target = xVreg;
            inter.remove(i + 1);
            i--;
        }
    }
}

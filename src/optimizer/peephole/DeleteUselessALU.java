package optimizer.peephole;

import intercode.Operand;
import intercode.Operand.InstNumber;
import intercode.Operand.VirtualReg;
import intercode.Quaternion;

import java.util.List;

import static intercode.Quaternion.OperatorType.*;

public class DeleteUselessALU {
    public static void run(List<Quaternion> inter) {
        for (int i = 0; i < inter.size(); i++) {
            Quaternion q = inter.get(i);
            // 0+x, x+0 -> set
            if (q.op == ADD) {
                if (isZero(q.x1))
                    inter.set(i, new Quaternion(SET, q.target, q.x2, null, null));
                else if (isZero(q.x2))
                     inter.set(i, new Quaternion(SET, q.target, q.x1, null, null));
            }
            // 0-x -> neg, x-0 -> set
            if (q.op == SUB) {
                if (isZero(q.x1))
                     inter.set(i, new Quaternion(NEG, q.target, q.x2, null, null));
                else if (isZero(q.x2))
                     inter.set(i, new Quaternion(SET, q.target, q.x1, null, null));
            }
            // 0*x, x*0, 0/x, 0%x -> set 0
            if (q.op == MULT || q.op == DIV || q.op == MOD) {
                // 默认不会有除 0 的情况
                if (isZero(q.x1) || isZero(q.x2))
                     inter.set(i, new Quaternion(SET, q.target, new InstNumber(0), null, null));
            }
            // 1*x, x*1 -> set x
            if (q.op == MULT) {
                if (isOne(q.x1))
                     inter.set(i, new Quaternion(SET, q.target, q.x2, null, null));
                else if (isOne(q.x2))
                     inter.set(i, new Quaternion(SET, q.target, q.x1, null, null));
                else if (isMinusOne(q.x1))
                     inter.set(i, new Quaternion(NEG, q.target, q.x2, null, null));
                else if (isMinusOne(q.x2))
                     inter.set(i, new Quaternion(NEG, q.target, q.x1, null, null));
            }
            // x/1 -> set x
            if (q.op == DIV) {
                if (isOne(q.x2))
                     inter.set(i, new Quaternion(SET, q.target, q.x1, null, null));
                if (isMinusOne(q.x2))
                     inter.set(i, new Quaternion(NEG, q.target, q.x1, null, null));
            }
            // x%1 -> set 0
            if (q.op == MOD) {
                if (isOne(q.x2) || isMinusOne(q.x2))
                     inter.set(i, new Quaternion(SET, q.target, new InstNumber(0), null, null));
            }
            // 最后处理 x <- x 的情况
            if (q.op == SET) {
                if (q.target == q.x1) {
                    inter.remove(i--);
                }
            }
        }
    }

    private static boolean isZero(Operand o) {
        if (o instanceof InstNumber) return ((InstNumber) o).number == 0;
        else return ((VirtualReg) o).realReg == 0;
    }

    private static boolean isOne(Operand o) {
        return o instanceof InstNumber && ((InstNumber) o).number == 1;
    }

    private static boolean isMinusOne(Operand o) {
        return o instanceof InstNumber && ((InstNumber) o).number == -1;
    }
}

package optimizer.peephole;

import intercode.InterCode;
import intercode.Operand;
import intercode.Operand.InstNumber;
import intercode.Operand.VirtualReg;
import intercode.Quaternion;

import static intercode.Quaternion.OperatorType.*;

public class DeleteUselessALU {
    public static void run(InterCode inter) {
        inter.forEachNode(p -> {
            // 0+x, x+0 -> set
            if (p.get().op == ADD) {
                if (isZero(p.get().x1))
                    p.set(new Quaternion(SET, p.get().target, p.get().x2, null, null));
                else if (isZero(p.get().x2))
                    p.set(new Quaternion(SET, p.get().target, p.get().x1, null, null));
            }
            // 0-x -> neg, x-0 -> set
            if (p.get().op == SUB) {
                if (isZero(p.get().x1))
                    p.set(new Quaternion(NEG, p.get().target, p.get().x2, null, null));
                else if (isZero(p.get().x2))
                    p.set(new Quaternion(SET, p.get().target, p.get().x1, null, null));
            }
            // 0*x, x*0, 0/x, 0%x -> set 0
            if (p.get().op == MULT || p.get().op == DIV || p.get().op == MOD) {
                // 默认不会有除 0 的情况
                if (isZero(p.get().x1) || isZero(p.get().x2))
                    p.set(new Quaternion(SET, p.get().target, new InstNumber(0), null, null));
            }
            // 1*x, x*1 -> set x
            if (p.get().op == MULT) {
                if (isOne(p.get().x1))
                    p.set(new Quaternion(SET, p.get().target, p.get().x2, null, null));
                else if (isOne(p.get().x2))
                    p.set(new Quaternion(SET, p.get().target, p.get().x1, null, null));
                else if (isMinusOne(p.get().x1))
                    p.set(new Quaternion(NEG, p.get().target, p.get().x2, null, null));
                else if (isMinusOne(p.get().x2))
                    p.set(new Quaternion(NEG, p.get().target, p.get().x1, null, null));
            }
            // x/1 -> set x
            if (p.get().op == DIV) {
                if (isOne(p.get().x2))
                    p.set(new Quaternion(SET, p.get().target, p.get().x1, null, null));
                if (isMinusOne(p.get().x2))
                    p.set(new Quaternion(NEG, p.get().target, p.get().x1, null, null));
            }
            // x%1 -> set 0
            if (p.get().op == MOD) {
                if (isOne(p.get().x2) || isMinusOne(p.get().x2))
                    p.set(new Quaternion(SET, p.get().target, new InstNumber(0), null, null));
            }
            // 最后处理 x <- x 的情况
            if (p.get().op == SET) {
                if (p.get().target == p.get().x1) p.delete();
            }
        });
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

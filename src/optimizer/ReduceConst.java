package optimizer;

import intercode.InterCode;
import intercode.Quaternion;

import java.util.Iterator;
import java.util.function.BiFunction;

import static intercode.Operand.InstNumber;
import static intercode.Operand.VirtualReg;
import static intercode.Quaternion.OperatorType.*;

public class ReduceConst {
    public static void run(InterCode inter) {
        for (Iterator<Quaternion> it = inter.list.iterator(); it.hasNext(); ) {
            Quaternion q = it.next();
            reduceReg(inter, q);
            if (q.op == SET) {
                if (q.x1 instanceof InstNumber) {
                    inter.setRegValue(q.target.reg, ((InstNumber) q.x1).number);
                    it.remove();
                }
            }
            else if (q.op == ADD || q.op == SUB || q.op == MULT || q.op == DIV || q.op == MOD) {
                if (q.x1 instanceof InstNumber && q.x2 instanceof InstNumber) {
                    BiFunction<Integer, Integer, Integer> binaryCalc = null;
                    if (q.op == ADD) binaryCalc = (a, b) -> a + b;
                    else if (q.op == SUB) binaryCalc = (a, b) -> a - b;
                    else if (q.op == MULT) binaryCalc = (a, b) -> a * b;
                    else if (q.op == DIV) binaryCalc = (a, b) -> a / b;
                    else if (q.op == MOD) binaryCalc = (a, b) -> a % b;
                    inter.setRegValue(q.target.reg, binaryCalc.apply(
                            ((InstNumber) q.x1).number,
                            ((InstNumber) q.x2).number));
                    it.remove();
                }
            }
        }
    }

    // 检查 x1, x2 的值是否能规约为常数，如果能，则规约它们
    private static void reduceReg(InterCode inter, Quaternion quater) {
        if (quater.x1 instanceof VirtualReg) {
            Integer num = inter.getRegValue(((VirtualReg) quater.x1).reg);
            if (num != null) {
                quater.x1 = new InstNumber(num);
            }
        }
        if (quater.x2 instanceof VirtualReg) {
            Integer num = inter.getRegValue(((VirtualReg) quater.x2).reg);
            if (num != null) {
                quater.x2 = new InstNumber(num);
            }
        }
    }
}

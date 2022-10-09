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

    }

    // 检查 x1, x2 的值是否能规约为常数，如果能，则规约它们
    private static void reduceReg(InterCode inter, Quaternion quater) {
        if (quater.x1 instanceof VirtualReg) {
            Integer num = inter.getRegValue(((VirtualReg) quater.x1).regID);
            if (num != null) {
                quater.x1 = new InstNumber(num);
            }
        }
        if (quater.x2 instanceof VirtualReg) {
            Integer num = inter.getRegValue(((VirtualReg) quater.x2).regID);
            if (num != null) {
                quater.x2 = new InstNumber(num);
            }
        }
    }
}

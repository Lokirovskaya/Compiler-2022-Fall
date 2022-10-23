package optimizer;

import intercode.InterCode;
import intercode.Operand.InstNumber;
import intercode.Quaternion;
import intercode.Quaternion.OperatorType;

import static intercode.Quaternion.OperatorType.*;

// 分支指令，转换成 GOTO 或删除：IF, IF_NOT
class MergeInst {
    public static void run(InterCode inter) {
        inter.forEach(p -> {
            // 一元
            if (p.get().x1 instanceof InstNumber && p.get().x2 == null) {
                OperatorType op = p.get().op;
                int x1 = ((InstNumber) p.get().x1).number;
                if (op == NOT) {
                    int ans = (x1 != 0) ? 0 : 1;
                    p.set(new Quaternion(SET, p.get().target, new InstNumber(ans), null, null));
                }
                else if (op == NEG) {
                    p.set(new Quaternion(SET, p.get().target, new InstNumber(-x1), null, null));
                }
                else if (op == IF) {
                    if (x1 != 0) p.set(new Quaternion(GOTO, null, null, null, p.get().label));
                    else p.delete();
                }
                else if (op == IF_NOT) {
                    if (x1 == 0) p.set(new Quaternion(GOTO, null, null, null, p.get().label));
                    else p.delete();
                }
            }
        });
    }
}

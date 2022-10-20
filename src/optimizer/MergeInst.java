package optimizer;

import intercode.InterCode;
import intercode.Operand.InstNumber;
import intercode.Quaternion;
import intercode.Quaternion.OperatorType;

import static intercode.Quaternion.OperatorType.*;

// 将操作数全部是立即数的指令进行计算，因为无法生成操作数都是立即数的 MIPS 代码
// 计算指令，转换成 SET：ADD, SUB, MULT, DIV, MOD, NOT, EQ, NOT_EQ, LESS, LESS_EQ, GREATER, GREATER_EQ,
// 分支指令，转换成 GOTO 或删除：IF, IF_NOT
public class MergeInst {
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
                else if (op == IF) {
                    if (x1 != 0) p.set(new Quaternion(GOTO, null, null, null, p.get().label));
                    else p.delete();
                }
                else if (op == IF_NOT) {
                    if (x1 == 0) p.set(new Quaternion(GOTO, null, null, null, p.get().label));
                    else p.delete();
                }
            }
            // 二元
            else if (p.get().x1 instanceof InstNumber && p.get().x2 instanceof InstNumber) {
                OperatorType op = p.get().op;
                int x1 = ((InstNumber) p.get().x1).number;
                int x2 = ((InstNumber) p.get().x2).number;
                Integer ans = null;
                if (op == ADD) ans = x1 + x2;
                else if (op == SUB) ans = x1 - x2;
                else if (op == MULT) ans = x1 * x2;
                else if (op == DIV) ans = x1 / x2;
                else if (op == MOD) ans = x1 % x2;
                else if (op == EQ) ans = x1 == x2 ? 1 : 0;
                else if (op == NOT_EQ) ans = x1 != x2 ? 1 : 0;
                else if (op == LESS) ans = x1 < x2 ? 1 : 0;
                else if (op == LESS_EQ) ans = x1 <= x2 ? 1 : 0;
                else if (op == GREATER) ans = x1 > x2 ? 1 : 0;
                else if (op == GREATER_EQ) ans = x1 >= x2 ? 1 : 0;
                if (ans != null) {
                    p.set(new Quaternion(SET, p.get().target, new InstNumber(ans), null,null));
                }
            }
        });
    }
}

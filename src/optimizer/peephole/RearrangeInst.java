package optimizer.peephole;

import intercode.Operand;
import intercode.Operand.InstNumber;
import intercode.Operand.VirtualReg;
import intercode.Quaternion;

import java.util.List;

import static intercode.Quaternion.OperatorType.*;

// Mips 有些指令不允许 x1 是立即数，x2 是寄存器，这里解决部分简单的情况，复杂的情况（如 sub, div）在最终代码生成中解决
public class RearrangeInst {
    public static void run(List<Quaternion> inter) {
        for (int i = 0; i < inter.size(); i++) {
            Quaternion q = inter.get(i);
            if (q.x1 instanceof InstNumber && q.x2 instanceof VirtualReg) {
                Quaternion.OperatorType op = q.op;
                if (op == ADD || op == MULT || op == EQ || op == NOT_EQ || op == GET_ARRAY) {
                    swapX1X2(q);
                }
                else if (op == LESS) {
                    swapX1X2(q);
                    q.op = GREATER;
                }
                else if (op == GREATER) {
                    swapX1X2(q);
                    q.op = LESS;
                }
                else if (op == LESS_EQ) {
                    swapX1X2(q);
                    q.op = GREATER_EQ;
                }
                else if (op == GREATER_EQ) {
                    swapX1X2(q);
                    q.op = LESS_EQ;
                }
            }
            else if (q.x1 instanceof InstNumber && q.x2 == null) {
                Quaternion.OperatorType op = q.op;
                int x1 = ((InstNumber) q.x1).number;
                if (op == IF) {
                    if (x1 != 0) q.op = GOTO;
                    else inter.remove(i--);
                }
                else if (op == IF_NOT) {
                    if (x1 == 0) q.op = GOTO;
                    else inter.remove(i--);
                }
            }
        }
    }

    private static void swapX1X2(Quaternion quater) {
        Operand temp = quater.x1;
        quater.x1 = quater.x2;
        quater.x2 = temp;
    }
}

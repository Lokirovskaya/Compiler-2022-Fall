package optimizer;

import intercode.InterCode;
import intercode.Operand;
import intercode.Operand.InstNumber;
import intercode.Operand.VirtualReg;
import intercode.Quaternion;

import static intercode.Quaternion.OperatorType.*;

// Mips 有些指令不允许 x1 是立即数，x2 是寄存器，这里解决部分简单的情况，复杂的情况（如 sub, div）在最终代码生成中解决
class RearrangeInst {
    static void run(InterCode inter) {
        inter.forEachNode(p -> {
            if (p.get().x1 instanceof InstNumber && p.get().x2 instanceof VirtualReg) {
                Quaternion.OperatorType op = p.get().op;
                if (op == ADD || op == MULT || op == EQ || op == NOT_EQ || op == GET_ARRAY) {
                    swapX1X2(p.get());
                }
                else if (op == LESS) {
                    swapX1X2(p.get());
                    p.get().op = GREATER;
                }
                else if (op == GREATER) {
                    swapX1X2(p.get());
                    p.get().op = LESS;
                }
                else if (op == LESS_EQ) {
                    swapX1X2(p.get());
                    p.get().op = GREATER_EQ;
                }
                else if (op == GREATER_EQ) {
                    swapX1X2(p.get());
                    p.get().op = LESS_EQ;
                }
            }
            else if (p.get().x1 instanceof InstNumber && p.get().x2 == null) {
                Quaternion.OperatorType op = p.get().op;
                int x1 = ((InstNumber) p.get().x1).number;
                if (op == IF) {
                    if (x1 != 0) p.get().op = GOTO;
                    else p.delete();
                }
                else if (op == IF_NOT) {
                    if (x1 == 0) p.get().op = GOTO;
                    else p.delete();
                }
            }
        });
    }

    private static void swapX1X2(Quaternion quater) {
        Operand temp = quater.x1;
        quater.x1 = quater.x2;
        quater.x2 = temp;
    }
}

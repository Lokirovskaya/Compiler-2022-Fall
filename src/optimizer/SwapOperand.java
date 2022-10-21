package optimizer;

import intercode.InterCode;
import intercode.Operand;
import intercode.Operand.InstNumber;
import intercode.Operand.VirtualReg;
import intercode.Quaternion;

import static intercode.Quaternion.OperatorType.*;

// 如果 x1 是立即数，x2 是 vreg，将其交换，可能有副作用（如 sub 和 div 等）
class SwapOperand {
    public static void run(InterCode inter) {
        inter.forEach(p -> {
            if (p.get().x1 instanceof InstNumber && p.get().x2 instanceof VirtualReg) {
                Quaternion.OperatorType op = p.get().op;
                if (op == ADD || op == MULT || op == EQ || op == NOT_EQ || op == GET_ARRAY) {
                    swapX1X2(p.get());
                }
                else if (op == SUB) {
                    swapX1X2(p.get());
                    p.insertNext(new Quaternion(NEG, p.get().target, p.get().target, null, null));
                }
                else if (op == DIV || op == MOD) {
                    VirtualReg reg = inter.newReg();
                    p.insertPrev(new Quaternion(SET, reg, p.get().x1, null, null));
                    p.get().x1 = reg;
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
        });
    }

    private static void swapX1X2(Quaternion quater) {
        Operand temp = quater.x1;
        quater.x1 = quater.x2;
        quater.x2 = temp;
    }
}

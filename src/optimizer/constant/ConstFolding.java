package optimizer.constant;

import intercode.Operand.InstNumber;
import intercode.Quaternion;

import static intercode.Quaternion.OperatorType.*;

class ConstFolding {
    // 将可以计算出的四元式改为 set 语句或 if 语句
    // 二元运算
    // ADD, SUB, MULT, DIV, MOD, EQ, NOT_EQ, LESS, LESS_EQ, GREATER, GREATER_EQ
    // IF_EQ, IF_NOT_EQ, IF_LESS, IF_LESS_EQ, IF_GREATER, IF_GREATER_EQ,
    // 一元运算：
    // NEG, NOT, IF_NOT
    static void foldQuater(Quaternion q) {
        if (q.x1 instanceof InstNumber && q.x2 instanceof InstNumber) {
            int x1 = ((InstNumber) q.x1).number;
            int x2 = ((InstNumber) q.x2).number;
            Integer ans = null;
            if (q.op == ADD) ans = x1 + x2;
            else if (q.op == SUB) ans = x1 - x2;
            else if (q.op == MULT) ans = x1 * x2;
            else if (q.op == DIV) {
                if (x2 == 0) ans = 0;
                else ans = x1 / x2;
            }
            else if (q.op == MOD) {
                if (x2 == 0) ans = 0;
                else ans = x1 % x2;
            }
            else if (q.op == EQ) ans = x1 == x2 ? 1 : 0;
            else if (q.op == NOT_EQ) ans = x1 != x2 ? 1 : 0;
            else if (q.op == LESS) ans = x1 < x2 ? 1 : 0;
            else if (q.op == LESS_EQ) ans = x1 <= x2 ? 1 : 0;
            else if (q.op == GREATER) ans = x1 > x2 ? 1 : 0;
            else if (q.op == GREATER_EQ) ans = x1 >= x2 ? 1 : 0;
            if (ans != null) {
                q.op = SET;
                q.x1 = new InstNumber(ans);
                q.x2 = null;
                return;
            }

            if (q.op == IF_EQ) ans = x1 == x2 ? 1 : 0;
            if (q.op == IF_NOT_EQ) ans = x1 != x2 ? 1 : 0;
            if (q.op == IF_LESS) ans = x1 < x2 ? 1 : 0;
            if (q.op == IF_LESS_EQ) ans = x1 <= x2 ? 1 : 0;
            if (q.op == IF_GREATER) ans = x1 > x2 ? 1 : 0;
            if (q.op == IF_GREATER_EQ) ans = x1 >= x2 ? 1 : 0;
            if (ans != null) {
                q.op = IF;
                q.x1 = new InstNumber(ans);
                q.x2 = null;
            }
        }
        else if (q.x1 instanceof InstNumber && q.x2 == null) {
            int x = ((InstNumber) q.x1).number;
            Integer ans = null;
            if (q.op == NEG) ans = -x;
            else if (q.op == NOT) ans = x != 0 ? 0 : 1;
            if (ans != null) {
                q.op = SET;
                q.x1 = new InstNumber(ans);
                return;
            }

            if (q.op == IF_NOT) {
                q.op = IF;
                q.x1 = new InstNumber(x != 0 ? 0 : 1);
            }
        }
    }
}

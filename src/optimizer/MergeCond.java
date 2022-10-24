package optimizer;

import intercode.InterCode;

import static intercode.Quaternion.OperatorType.*;

// 将判断语句合并到分支语句中去
// todo: IF_COND 语句不会在 merge inst 和 swap operand 中调整
class MergeCond {
    static void run(InterCode inter) {
        inter.forEach(p -> {
            switch (p.get().op) {
                case NOT:
                    if (p.get(1).op == IF) p.get(1).op = IF_NOT;
                    else if (p.get(1).op == IF_NOT) p.get(1).op = IF;
                    else break;
                    p.get(1).x1 = p.get().x1;
                    p.delete();
                    break;
                case EQ:
                    if (p.get(1).op == IF) p.get(1).op = IF_EQ;
                    else if (p.get(1).op == IF_NOT) p.get(1).op = IF_NOT_EQ;
                    else break;
                    p.get(1).x1 = p.get().x1;
                    p.get(1).x2 = p.get().x2;
                    p.delete();
                    break;
                case NOT_EQ:
                    if (p.get(1).op == IF) p.get(1).op = IF_NOT_EQ;
                    else if (p.get(1).op == IF_NOT) p.get(1).op = IF_EQ;
                    else break;
                    p.get(1).x1 = p.get().x1;
                    p.get(1).x2 = p.get().x2;
                    p.delete();
                    break;
                case LESS:
                    if (p.get(1).op == IF) p.get(1).op = IF_LESS;
                    else if (p.get(1).op == IF_NOT) p.get(1).op = IF_GREATER_EQ;
                    else break;
                    p.get(1).x1 = p.get().x1;
                    p.get(1).x2 = p.get().x2;
                    p.delete();
                    break;
                case LESS_EQ:
                    if (p.get(1).op == IF) p.get(1).op = IF_LESS_EQ;
                    else if (p.get(1).op == IF_NOT) p.get(1).op = IF_GREATER;
                    else break;
                    p.get(1).x1 = p.get().x1;
                    p.get(1).x2 = p.get().x2;
                    p.delete();
                    break;
                case GREATER:
                    if (p.get(1).op == IF) p.get(1).op = IF_GREATER;
                    else if (p.get(1).op == IF_NOT) p.get(1).op = IF_LESS_EQ;
                    else break;
                    p.get(1).x1 = p.get().x1;
                    p.get(1).x2 = p.get().x2;
                    p.delete();
                    break;
                case GREATER_EQ:
                    if (p.get(1).op == IF) p.get(1).op = IF_GREATER_EQ;
                    else if (p.get(1).op == IF_NOT) p.get(1).op = IF_LESS;
                    else break;
                    p.get(1).x1 = p.get().x1;
                    p.get(1).x2 = p.get().x2;
                    p.delete();
                    break;
            }
        });
    }
}

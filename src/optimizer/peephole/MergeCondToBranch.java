package optimizer.peephole;


import intercode.Quaternion;

import java.util.List;

import static intercode.Quaternion.OperatorType.*;

// 将判断语句合并到分支语句中去
public class MergeCondToBranch {
    public static void run(List<Quaternion> inter) {
        for (int i = 0; i < inter.size() - 1; i++) {
            Quaternion q = inter.get(i);
            switch (q.op) {
                case NOT:
                    if (inter.get(i + 1).op == IF) inter.get(i + 1).op = IF_NOT;
                    else if (inter.get(i + 1).op == IF_NOT) inter.get(i + 1).op = IF;
                    else break;
                    inter.get(i + 1).x1 = q.x1;
                    inter.remove(i--);
                    break;
                case EQ:
                    if (inter.get(i + 1).op == IF) inter.get(i + 1).op = IF_EQ;
                    else if (inter.get(i + 1).op == IF_NOT) inter.get(i + 1).op = IF_NOT_EQ;
                    else break;
                    inter.get(i + 1).x1 = q.x1;
                    inter.get(i + 1).x2 = q.x2;
                    inter.remove(i--);
                    break;
                case NOT_EQ:
                    if (inter.get(i + 1).op == IF) inter.get(i + 1).op = IF_NOT_EQ;
                    else if (inter.get(i + 1).op == IF_NOT) inter.get(i + 1).op = IF_EQ;
                    else break;
                    inter.get(i + 1).x1 = q.x1;
                    inter.get(i + 1).x2 = q.x2;
                    inter.remove(i--);
                    break;
                case LESS:
                    if (inter.get(i + 1).op == IF) inter.get(i + 1).op = IF_LESS;
                    else if (inter.get(i + 1).op == IF_NOT) inter.get(i + 1).op = IF_GREATER_EQ;
                    else break;
                    inter.get(i + 1).x1 = q.x1;
                    inter.get(i + 1).x2 = q.x2;
                    inter.remove(i--);
                    break;
                case LESS_EQ:
                    if (inter.get(i + 1).op == IF) inter.get(i + 1).op = IF_LESS_EQ;
                    else if (inter.get(i + 1).op == IF_NOT) inter.get(i + 1).op = IF_GREATER;
                    else break;
                    inter.get(i + 1).x1 = q.x1;
                    inter.get(i + 1).x2 = q.x2;
                    inter.remove(i--);
                    break;
                case GREATER:
                    if (inter.get(i + 1).op == IF) inter.get(i + 1).op = IF_GREATER;
                    else if (inter.get(i + 1).op == IF_NOT) inter.get(i + 1).op = IF_LESS_EQ;
                    else break;
                    inter.get(i + 1).x1 = q.x1;
                    inter.get(i + 1).x2 = q.x2;
                    inter.remove(i--);
                    break;
                case GREATER_EQ:
                    if (inter.get(i + 1).op == IF) inter.get(i + 1).op = IF_GREATER_EQ;
                    else if (inter.get(i + 1).op == IF_NOT) inter.get(i + 1).op = IF_LESS;
                    else break;
                    inter.get(i + 1).x1 = q.x1;
                    inter.get(i + 1).x2 = q.x2;
                    inter.remove(i--);
                    break;
            }
        }
    }
}

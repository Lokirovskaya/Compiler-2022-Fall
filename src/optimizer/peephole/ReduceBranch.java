package optimizer.peephole;

import intercode.Quaternion;
import optimizer.misc.ClearLabel;

import java.util.List;

import static intercode.Quaternion.OperatorType.*;

public class ReduceBranch {
    public static void run(List<Quaternion> inter) {
        for (int i = 0; i < inter.size(); i++) {
            Quaternion q = inter.get(i);

            // [if cond] goto A; {label X}; label A; -> {label X}; label A;
            if (i + 1 < inter.size() &&
                    (q.op == GOTO || q.op == IF || q.op == IF_NOT) &&
                    inter.get(i + 1).op == LABEL) {
                int k = 1;
                while (i + k < inter.size() && inter.get(i + k).op == LABEL) {
                    if (q.label.name.equals(inter.get(i + k).label.name)) {
                        inter.remove(i--);
                        break;
                    }
                    k++;
                }
            }

            // if cond goto A; goto B; label A; -> if_not cond goto B; label A;
            if (i + 2 < inter.size() &&
                    (q.op == IF || q.op == IF_NOT) &&
                    inter.get(i + 1).op == GOTO && inter.get(i + 2).op == LABEL &&
                    q.label == inter.get(i + 2).label) {
                q.op = (q.op == IF) ? IF_NOT : IF;
                q.label = inter.get(i + 1).label;
                inter.remove(i + 1);
            }

            // if cond goto A; goto A; -> goto A;
            if (i + 1 < inter.size() &&
                    (q.op == IF || q.op == IF_NOT) &&
                    inter.get(i + 1).op == GOTO &&
                    q.label == inter.get(i + 1).label) {
                inter.remove(i--);
            }
        }
        ClearLabel.run(inter);
    }
}

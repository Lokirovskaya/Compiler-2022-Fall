package optimizer;

import intercode.InterCode;

import static intercode.Quaternion.OperatorType.*;

class ReduceGoto {
    // goto A; label A; -> label A;
    // if cond goto A; label A; -> label A;
    // if cond goto A; goto B; label A; -> if_not cond goto B; label A;
    static void run(InterCode inter) {
        inter.forEach(p -> {
            if (p.get(2) != null &&
                    (p.get(1).op == GOTO || p.get(1).op == IF || p.get(1).op == IF_NOT) &&
                    p.get(2).op == LABEL && p.get(1).label == p.get(2).label) {
                p.deleteNext();
            }
            else if (p.get(2) != null &&
                    (p.get(0).op == IF || p.get(0).op == IF_NOT) &&
                    p.get(1).op == GOTO && p.get(2).op == LABEL &&
                    p.get(0).label == p.get(2).label) {
                p.get(0).op = (p.get(0).op == IF) ? IF_NOT : IF;
                p.get(0).label = p.get(1).label;
                p.deleteNext();
            }
        });
    }
}

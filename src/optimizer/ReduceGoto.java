package optimizer;

import intercode.InterCode;

import static intercode.Quaternion.OperatorType.*;

class ReduceGoto {
    static void run(InterCode inter) {
        inter.forEach(p -> {
            while (true) {
                // [if cond] goto A; {label X}; label A; -> {label X}; label A;
                if (p.get(1) != null &&
                        (p.get().op == GOTO || p.get().op == IF || p.get().op == IF_NOT) &&
                        p.get(1).op == LABEL) {
                    int i = 1;
                    boolean found = false;
                    while (p.get(i) != null && p.get(i).op == LABEL) {
                        if (p.get().label == p.get(i).label) {
                            p.delete();
                            found = true;
                            break;
                        }
                        i++;
                    }
                    if (found) continue;
                }
                // if cond goto A; goto B; label A; -> if_not cond goto B; label A;
                if (p.get(2) != null &&
                        (p.get().op == IF || p.get().op == IF_NOT) &&
                        p.get(1).op == GOTO && p.get(2).op == LABEL &&
                        p.get().label == p.get(2).label) {
                    p.get().op = (p.get().op == IF) ? IF_NOT : IF;
                    p.get().label = p.get(1).label;
                    p.delete(1);
                    continue;
                }
                // if cond goto A; goto A; -> goto A;
                if (p.get(1) != null &&
                        (p.get().op == IF || p.get().op == IF_NOT) &&
                        p.get(1).op == GOTO &&
                        p.get().label == p.get(1).label) {
                    p.delete();
                    continue;
                }
                break;
            }
        });
    }
}

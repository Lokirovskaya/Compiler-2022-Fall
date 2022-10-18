package optimizer;

import intercode.InterCode;
import intercode.Label;

import java.util.HashMap;
import java.util.Map;

import static intercode.Quaternion.OperatorType.*;

class ReduceGoto {
    static void run(InterCode inter) {
        initLabelRef(inter);
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
                            int ref = reduceLabelRef(p.get(i).label);
                            if (ref == 0) p.delete(i);
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
                    int ref = reduceLabelRef(p.get().label);
                    if (ref == 0) p.delete(2);
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

    private static final Map<Label, Integer> labelRef = new HashMap<>();

    private static void initLabelRef(InterCode inter) {
        inter.forEach(p -> {
            if (p.get().op == IF || p.get().op == IF_NOT || p.get().op == GOTO) {
                labelRef.computeIfPresent(p.get().label, (k, v) -> v + 1);
                labelRef.putIfAbsent(p.get().label, 1);
            }
        });
    }

    private static int reduceLabelRef(Label label) {
        labelRef.computeIfPresent(label, (k, v) -> v - 1);
        return labelRef.get(label);
    }
}

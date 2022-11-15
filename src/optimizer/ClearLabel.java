package optimizer;

import intercode.Label;
import intercode.Quaternion;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static intercode.Quaternion.OperatorType.*;

class ClearLabel {
    static void run(List<Quaternion> inter) {
        Set<Label> labelRef = new HashSet<>();
        for (Quaternion q : inter) {
            if (q.op == GOTO || q.op == IF || q.op == IF_NOT || q.op == IF_EQ || q.op == IF_NOT_EQ
                    || q.op == IF_LESS || q.op == IF_GREATER || q.op == IF_LESS_EQ || q.op == IF_GREATER_EQ)
                labelRef.add(q.label);
        }
        inter.removeIf(q -> q.op == LABEL && !labelRef.contains(q.label));
    }
}

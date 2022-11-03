package optimizer;

import intercode.InterCode;
import intercode.Label;

import java.util.HashSet;
import java.util.Set;

import static intercode.Quaternion.OperatorType.*;

class ClearLabel {
    static void run(InterCode inter) {
        Set<Label> labelRef = new HashSet<>();
        inter.forEachItem(quater -> {
            if (quater.op == GOTO || quater.op == IF || quater.op == IF_NOT || quater.op == IF_EQ || quater.op == IF_NOT_EQ
                    || quater.op == IF_LESS || quater.op == IF_GREATER || quater.op == IF_LESS_EQ || quater.op == IF_GREATER_EQ)
                labelRef.add(quater.label);
        });
        inter.forEachNode(p -> {
            if (p.get().op == LABEL && !labelRef.contains(p.get().label)) p.delete();
        });
    }
}

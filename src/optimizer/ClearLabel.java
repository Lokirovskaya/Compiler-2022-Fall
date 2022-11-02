package optimizer;

import intercode.InterCode;
import intercode.Label;

import java.util.HashSet;
import java.util.Set;

import static intercode.Quaternion.OperatorType.*;

class ClearLabel {
    static void run(InterCode inter) {
        Set<Label> labelRef = new HashSet<>();
        inter.forEach(p -> {
            if (p.get().op == GOTO || p.get().op == IF || p.get().op == IF_NOT || p.get().op == IF_EQ || p.get().op == IF_NOT_EQ
                    || p.get().op == IF_LESS || p.get().op == IF_GREATER || p.get().op == IF_LESS_EQ || p.get().op == IF_GREATER_EQ)
                labelRef.add(p.get().label);
        });
        inter.forEach(p -> {
            if (p.get().op == LABEL && !labelRef.contains(p.get().label)) p.delete();
        });
    }
}

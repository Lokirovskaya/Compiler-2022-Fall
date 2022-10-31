package optimizer;

import intercode.InterCode;
import intercode.Label;

import java.util.HashSet;
import java.util.Set;

import static intercode.Quaternion.OperatorType.*;

class ClearLabel {
    static InterCode run(InterCode inter) {
        Set<Label> labelRef = new HashSet<>();
        inter.forEach(p -> {
            if (p.get().op == IF || p.get().op == IF_NOT || p.get().op == GOTO) {
                labelRef.add(p.get().label);
            }
        });
        inter.forEach(p -> {
            if (p.get().op == LABEL && !labelRef.contains(p.get().label)) p.delete();
        });
        return inter;
    }
}

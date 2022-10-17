package optimizer;

import intercode.InterCode;
import intercode.Operand;

import java.util.HashSet;
import java.util.Set;

import static intercode.Quaternion.OperatorType.*;

class DeleteUnused {
    static void run(InterCode inter) {
        deleteUnusedLabel(inter);
    }

    private static void deleteUnusedLabel(InterCode inter) {
        Set<Operand.Label> labelRef = new HashSet<>();
        inter.forEach(p -> {
            if (p.get().op == IF || p.get().op == IF_NOT || p.get().op == GOTO) {
                labelRef.add(p.get().label);
            }
        });
        inter.forEach(p -> {
            if (p.get().op == LABEL) {
                if (!labelRef.contains(p.get().label)) p.delete();
            }
        });
    }
}

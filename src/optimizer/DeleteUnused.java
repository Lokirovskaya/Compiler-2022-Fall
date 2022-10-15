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
            if (p.get(0).op == IF || p.get(0).op == IF_NOT || p.get(0).op == GOTO) {
                labelRef.add(p.get(0).label);
            }
        });
        inter.forEach(p -> {
            if (p.get(1) != null && p.get(1).op == LABEL) {
                if (!labelRef.contains(p.get(1).label)) p.deleteNext();
            }
        });
    }
}

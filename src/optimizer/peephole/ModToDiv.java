package optimizer.peephole;

import intercode.Operand;
import intercode.Operand.InstNumber;
import intercode.Operand.VirtualReg;
import intercode.Quaternion;
import intercode.VirtualRegFactory;

import java.util.ArrayList;
import java.util.List;

import static intercode.Quaternion.OperatorType.*;

// t=a%mod -> t=a-a/mod*mod
public class ModToDiv {
    public static void run(List<Quaternion> inter) {
        for (int i = 0; i < inter.size(); i++) {
            Quaternion q = inter.get(i);
            if (q.op == MOD && q.x2 instanceof InstNumber) {
                InstNumber inst = new InstNumber(Math.abs(((InstNumber) q.x2).number));
                VirtualReg divAns = VirtualRegFactory.newReg();
                VirtualReg multAns = VirtualRegFactory.newReg();

                List<Quaternion> modInter = new ArrayList<>();
                modInter.add(new Quaternion(DIV, divAns, q.x1, inst, null));
                modInter.add(new Quaternion(MULT, multAns, divAns, inst, null));
                modInter.add(new Quaternion(SUB, q.target, q.x1, multAns, null));

                inter.remove(i--);
                inter.addAll(i + 1, modInter);
            }
        }
    }
}

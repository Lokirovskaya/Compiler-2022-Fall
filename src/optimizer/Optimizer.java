package optimizer;

import intercode.Quaternion;
import optimizer.peephole.*;
import optimizer.register.RegAlloc;

import java.util.List;

public class Optimizer {
    public static void optimize(List<Quaternion> inter) {
        ReduceBranch.run(inter);
        ClearLabel.run(inter);
        RearrangeInst.run(inter);
        MergeCondToBranch.run(inter);
        ClearDeadCode.run(inter);
        ClearLabel.run(inter);

        MergePrint.run(inter);
        DeleteUselessALU.run(inter);

        RegAlloc.run(inter);
    }

}

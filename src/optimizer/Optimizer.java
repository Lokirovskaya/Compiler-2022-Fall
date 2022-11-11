package optimizer;

import intercode.InterCode;
import optimizer.peephole.*;
import optimizer.register.RegAlloc;

public class Optimizer {
    public static void optimize(InterCode inter) {
        ReduceBranch.run(inter);
        ClearLabel.run(inter);
        RearrangeInst.run(inter);
        MergeCondToBranch.run(inter);
        ClearDeadCode.run(inter);
        ReduceBranch.run(inter);
        ClearLabel.run(inter);

        MergePrint.run(inter);
        WeakenALU.run(inter);

        RegAlloc.run(inter);
    }

}

package optimizer;

import intercode.Quaternion;
import optimizer.peephole.*;
import optimizer.register.RegAlloc;

import java.util.List;

public class Optimizer {
    public static void optimize(List<Quaternion> inter) {
        MoveMainFunc.run(inter);

        ReduceBranch.run(inter);
        ClearLabel.run(inter);

        RearrangeInst.run(inter);
        MergeCondToBranch.run(inter);

        ClearUnusedVar.run(inter);
        ClearUnusedVar.run(inter);

        ClearDeadCode.run(inter);
        ClearLabel.run(inter);

        MergePrint.run(inter);
        WeakenRedundantCalc.run(inter);

        RegAlloc.run(inter);
    }

}

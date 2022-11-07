package optimizer;

import intercode.InterCode;

public class Optimizer {
    public static void optimize(InterCode inter) {
        ReduceBranch.run(inter);
        ClearLabel.run(inter);
        RearrangeInst.run(inter);
        MergeCondToBranch.run(inter);
        ClearDeadCode.run(inter);
        ReduceBranch.run(inter);
        MergePrint.run(inter);
        ClearLabel.run(inter);
        // 请保证 RegAlloc 是最后一步
//        RegAlloc.run(inter);
    }
}

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
        ClearLabel.run(inter);
    }
}

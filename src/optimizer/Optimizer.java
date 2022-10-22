package optimizer;

import intercode.InterCode;

public class Optimizer {
    public static void optimize(InterCode inter) {
        ReduceGoto.run(inter);
        ClearLabel.run(inter);
        MergeCond.run(inter);
        MipsPreprocess(inter);
    }

    // 必做的步骤，否则难以生成 mips 代码
    public static void MipsPreprocess(InterCode inter) {
        MergeInst.run(inter);
        SwapOperand.run(inter);
    }
}

package optimizer;

import intercode.InterCode;

public class Optimizer {
    public static void optimize(InterCode inter) {
        ReduceGoto.run(inter);
        ClearLabel.run(inter);
        RearrangeInst.run(inter);
        MergeCond.run(inter);
    }
}

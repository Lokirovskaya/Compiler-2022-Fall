package optimizer;

import intercode.InterCode;
import optimizer.block.FuncBlocks;
import optimizer.block.LivenessAnalysis;
import optimizer.block.SplitBlock;

import java.util.List;

public class Optimizer {
    public static void optimize(InterCode inter) {
        ReduceBranch.run(inter);
        ClearLabel.run(inter);
        RearrangeInst.run(inter);
        MergeCondToBranch.run(inter);



        List<FuncBlocks> list = SplitBlock.split(inter);
        list.forEach(f-> LivenessAnalysis.run(f));
        list.forEach(f -> f.printFuncBlocks());

        ClearDeadCode.run(inter);
        ReduceBranch.run(inter);
        MergePrint.run(inter);
        ClearLabel.run(inter);
    }
}

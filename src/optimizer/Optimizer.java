package optimizer;

import intercode.InterCode;
import optimizer.block.Block;
import optimizer.block.SplitBlock;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;

public class Optimizer {
    private static final List<Function<InterCode, InterCode>> optimizers = new ArrayList<>();

    static {
        optimizers.add(ReduceBranch::run);
        optimizers.add(ClearLabel::run);
        optimizers.add(RearrangeInst::run);
        optimizers.add(MergeCondToBranch::run);
        optimizers.add(ClearLabel::run);
    }

    public static InterCode optimize(InterCode inter) {
        for (Function<InterCode, InterCode> func : optimizers) {
            inter = func.apply(inter);
        }
//
//        List<Block> blockList = SplitBlock.run(inter);
//        for (Block block : blockList) {
//            System.out.println(block);
//        }
        return inter;
    }
}

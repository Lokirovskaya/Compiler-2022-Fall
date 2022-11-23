package optimizer;

import intercode.Quaternion;
import optimizer.constant.ConstPropagationBlock;
import optimizer.misc.*;
import optimizer.peephole.*;
import optimizer.register.*;

import java.util.List;

public class OptimizeInter {
    public static void optimize(List<Quaternion> inter) {
        MoveMainFunc.run(inter);

        ReduceBranch.run(inter);

        RearrangeInst.run(inter);
        MergeCondToBranch.run(inter);

//        InlineFunc.run(inter);
        ReduceBranch.run(inter);

        ClearDeadCode.run(inter);

        ClearUselessAssign.run(inter);
        CommonSubexpElim.run(inter);

        for (int i = 0; i < 2; i++) {
            ConstPropagationBlock.run(inter);
            ClearDeadCode.run(inter);
        }

        ReduceBranch.run(inter);
        MergePrint.run(inter);
        WeakenRedundantCalc.run(inter);

        ClearUselessAssign.run(inter);

        RegAlloc.run(inter);
    }

}

package optimizer;

import intercode.Quaternion;
import optimizer.constant.*;
import optimizer.misc.InlineFunc;
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

        InlineFunc.run(inter);
        ReduceBranch.run(inter);

        ClearDeadCode.run(inter);

        CopyPropagation.run(inter);
        ClearUselessAssign.run(inter);

        CommonSubexpElim.run(inter);

        CopyPropagation.run(inter);
        ClearUselessAssign.run(inter);

        final int pass = 2;
        for (int i = 0; i < pass; i++) {
            ConstPropagationBlock.run(inter);
            ClearDeadCode.run(inter);
        }
        for (int i = 0; i < pass; i++) {
            ConstPropagationBlock.run(inter);
            ClearDeadCode.run(inter);
        }

        ReduceBranch.run(inter);
        MergePrint.run(inter);

        WeakenRedundantCalc.run(inter);
        ModToDiv.run(inter);

        CommonSubexpElim.run(inter);

        ClearUselessAssign.run(inter);

        RegAlloc.run(inter);
    }

}

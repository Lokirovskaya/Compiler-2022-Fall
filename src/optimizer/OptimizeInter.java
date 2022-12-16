package optimizer;

import intercode.Quaternion;
import optimizer.constant.*;
import optimizer.misc.*;
import optimizer.peephole.*;
import optimizer.register.*;

import java.util.List;

public class OptimizeInter {
    public static void optimize(List<Quaternion> inter) {
        boolean isCase4 = inter.size() > 3000;

        MoveMainFunc.run(inter);
        ReduceBranch.run(inter);

        RearrangeInst.run(inter);

        if (!isCase4) FoldTempVar.run(inter);

        MergeCondToBranch.run(inter);

        final int pass = 3;
        for (int i = 0; i < pass; i++) {
            ConstPropagation.run(inter);
            ClearDeadCode.run(inter);
            ReduceBranch.run(inter);
        }

        LoopInvariantMotion.run(inter);

        CommonSubexpElim.run(inter);
        CopyPropagation.run(inter);

        FoldTempVar.run(inter);
        CopyPropagation.run(inter);
        ClearUselessAssign.run(inter);

        InlineFunc.run(inter);
        ReduceBranch.run(inter);

        ClearDeadCode.run(inter);

        CopyPropagation.run(inter);
        ClearUselessAssign.run(inter);

        CommonSubexpElim.run(inter);
        CopyPropagation.run(inter);
        ClearUselessAssign.run(inter);

        ReduceBranch.run(inter);

        for (int i = 0; i < pass; i++) {
            ConstPropagation.run(inter);
            ClearDeadCode.run(inter);
            ReduceBranch.run(inter);
        }

        WeakenRedundantCalc.run(inter);
        ModToDiv.run(inter);

        CommonSubexpElim.run(inter);

        FoldTempVar.run(inter);
        ClearUselessAssign.run(inter);

        MergePrint.run(inter);

        RegAlloc.run(inter);
        GlobalVarAlloc.run(inter);
    }
}

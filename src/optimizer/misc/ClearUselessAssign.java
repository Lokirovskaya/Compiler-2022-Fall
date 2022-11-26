package optimizer.misc;

import intercode.Operand.VirtualReg;
import intercode.Quaternion;
import optimizer.block.Block;
import optimizer.block.FuncBlocks;
import optimizer.block.LivenessAnalysis;
import optimizer.block.SplitBlock;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static intercode.Quaternion.OperatorType.*;

public class ClearUselessAssign {
    public static void run(List<Quaternion> inter) {
        List<FuncBlocks> funcBlocksList = SplitBlock.split(inter);
        for (FuncBlocks funcBlocks : funcBlocksList) {
            runClear(funcBlocks);
        }
        SplitBlock.join(funcBlocksList, inter);
    }

    // 获得每个基本块的 out
    // 倒序遍历基本块中的语句，求出语句级的 out，方法：每结束遍历一条语句，将它使用的所有变量加入 out 中
    // 若某个语句的 target 不在语句的 out 中，它就是无用的赋值
    private static void runClear(FuncBlocks funcBlocks) {
        LivenessAnalysis.doAnalysis(funcBlocks);

        for (Block block : funcBlocks.blockList) {
            Set<VirtualReg> out = new HashSet<>(block.livenessFlow.out);

            for (int i = block.blockInter.size() - 1; i >= 0; i--) {
                Quaternion q = block.blockInter.get(i);
                if (q.target != null && q.op != SET_ARRAY) {
                    if (!q.target.isGlobal && !out.contains(q.target)) {
                        q.isUselessAssign = true;
                    }
                }
                out.addAll(q.getUseVregList());
            }

            block.blockInter.removeIf(q -> q.isUselessAssign && q.op != GETINT);
        }
    }
}

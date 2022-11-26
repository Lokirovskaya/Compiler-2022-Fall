package optimizer.misc;

import intercode.Operand.VirtualReg;
import intercode.Quaternion;
import optimizer.block.Block;
import optimizer.block.FuncBlocks;
import optimizer.block.SplitBlock;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static intercode.Quaternion.OperatorType.*;

public class CopyPropagation {
    public static void run(List<Quaternion> inter) {
        List<FuncBlocks> funcBlocksList = SplitBlock.split(inter);
        for (FuncBlocks funcBlocks : funcBlocksList) {
            for (Block block : funcBlocks.blockList) {
                runCopyPropagation(block.blockInter);
            }
        }

        SplitBlock.join(funcBlocksList, inter);
    }

    private static void runCopyPropagation(List<Quaternion> inter) {
        Map<VirtualReg, VirtualReg> vregMap = new HashMap<>();
        for (Quaternion q : inter) {
            // kill 掉所有的涉及 def 的赋值
            for (VirtualReg def : q.getDefVregList()) {
                vregMap.entrySet().removeIf(e -> def.equals(e.getKey()) || def.equals(e.getValue()));
            }

            // 遇到 def，kill 掉所有涉及全局变量的寄存器
            if (q.op == CALL) {
                vregMap.entrySet().removeIf(e -> e.getKey().isGlobal || e.getValue().isGlobal);
            }

            if (q.op == SET) {
                if (q.x1 instanceof VirtualReg) {
                    vregMap.put(q.target, (VirtualReg) q.x1);
                }
            }
            else {
                // 将判断 q 使用的所有 vreg 有无可替换的
                // 参考 q.getUseVregList
                if (q.x1 instanceof VirtualReg && vregMap.containsKey(q.x1))
                    q.x1 = vregMap.get(q.x1);
                if (q.x2 instanceof VirtualReg && vregMap.containsKey(q.x2))
                    q.x2 = vregMap.get(q.x2);
                if (q.op != FUNC && q.list != null) {
                    for (int i = 0; i < q.list.size(); i++) {
                        if ((q.list.get(i)) instanceof VirtualReg && vregMap.containsKey((VirtualReg) q.list.get(i)))
                            q.list.set(i, vregMap.get((VirtualReg) q.list.get(i)));
                    }
                }
            }
        }
    }
}

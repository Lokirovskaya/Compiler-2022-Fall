package optimizer.constant;

import intercode.Operand.InstNumber;
import intercode.Operand.VirtualReg;
import intercode.Quaternion;
import optimizer.block.Block;
import optimizer.block.FuncBlocks;
import optimizer.block.SplitBlock;
import optimizer.peephole.RearrangeInst;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static intercode.Quaternion.OperatorType.*;

public class ConstPropagationBlock {
    public static void run(List<Quaternion> inter) {
        List<FuncBlocks> funcBlocksList = SplitBlock.split(inter);
        for (FuncBlocks funcBlocks : funcBlocksList) {
            for (Block block : funcBlocks.blockList) {
                runBlockInter(block.blockInter);
                RearrangeInst.run(block.blockInter);
            }
        }
        inter.clear();
        funcBlocksList.forEach(func -> func.blockList.forEach(block -> inter.addAll(block.blockInter)));
    }

    private static void runBlockInter(List<Quaternion> inter) {
        Map<VirtualReg, Integer> vregConstMap = new HashMap<>();
        for (Quaternion q : inter) {
            // 将判断 q 使用的所有 vreg 有无可替换为立即数的
            // 参考 q.getUseVregList
            if (q.x1 instanceof VirtualReg && vregConstMap.containsKey(q.x1))
                q.x1 = new InstNumber(vregConstMap.get(q.x1));
            if (q.x2 instanceof VirtualReg && vregConstMap.containsKey(q.x2))
                q.x2 = new InstNumber(vregConstMap.get(q.x2));
            if (q.op != FUNC && q.list != null) {
                for (int i = 0; i < q.list.size(); i++) {
                    if ((q.list.get(i)) instanceof VirtualReg && vregConstMap.containsKey((VirtualReg) q.list.get(i)))
                        q.list.set(i, new InstNumber(vregConstMap.get((VirtualReg) q.list.get(i))));
                }
            }

            // 折叠常量
            ConstFolding.foldQuater(q);

            // 如果出现了 set def inst 语句，将 def 加入字典中
            // 注意，全局变量也会被加入字典
            if (q.op == SET && q.x1 instanceof InstNumber) {
                int c = ((InstNumber) q.x1).number;
                vregConstMap.put(q.target, c);
            }
            // 如果遇到 call，将所有全局变量移除字典
            else if (q.op == CALL) {
                vregConstMap.entrySet().removeIf(entry -> entry.getKey().isGlobal);
            }

            // 如果 use 中依然存在不定值（vreg），将 def 移出字典
            if (q.getUseVregList().size() > 0 || q.op == GETINT) {
                q.getDefVregList().forEach(def -> vregConstMap.remove(def));
            }
        }
    }

}

package optimizer.block;

import intercode.Operand.VirtualReg;
import intercode.Quaternion;

import java.util.*;

import static intercode.Quaternion.OperatorType.SET_ARRAY;

class ReachDefAnalysis {
    static void doAnalysis(List<Block> blockList) {
        // 找到所有的定值语句
        // 参数不算做被定值
        Map<VirtualReg, List<Quaternion>> vregDefQuatersMap = new HashMap<>();
        for (Block block : blockList) {
            block.reachDefFlow = new Block.ReachDefFlow();
            for (Quaternion q : block.blockInter) {
                if (q.target != null && q.op != SET_ARRAY && !q.target.isGlobal) {
                    vregDefQuatersMap.putIfAbsent(q.target, new ArrayList<>());
                    vregDefQuatersMap.get(q.target).add(q);
                }
            }
        }

        // 计算 gen 和 kill
        for (Block block : blockList) {
            for (Quaternion q : block.blockInter) {
                if (q.target != null && q.op != SET_ARRAY && !q.target.isGlobal) {
                    block.reachDefFlow.gen.add(q);
                    for (Quaternion defToKill : vregDefQuatersMap.get(q.target)) {
                        if (defToKill != q) block.reachDefFlow.kill.add(defToKill);
                    }
                }
            }
        }

        // worklist 算法
        Deque<Block> changed = new ArrayDeque<>(blockList);
        while (!changed.isEmpty()) {
            Block block = changed.pollFirst();
            // in[B] = ∪_all out[B'] (B' = parent of B)
            for (Block parent : block.parents) {
                block.reachDefFlow.in.addAll(parent.reachDefFlow.out);
            }
            // out = gen ∪ (in - kill)
            Set<Quaternion> outAns = new HashSet<>(block.reachDefFlow.in);
            outAns.removeAll(block.reachDefFlow.kill);
            outAns.addAll(block.reachDefFlow.gen);
            // out[B] set changed?
            if (!outAns.equals(block.reachDefFlow.out)) {
                changed.addAll(block.parents);
            }
            block.reachDefFlow.out = outAns;
        }
    }
}

package optimizer.block;

import intercode.Operand;
import intercode.Quaternion;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

class LivenessAnalysis {
    static void doAnalysis(List<Block> blockList) {
        // 计算所有 block 的 use 和 def
        for (Block block : blockList) {
            block.livenessFlow = new Block.LivenessFlow();

            for (Quaternion q : block.blockInter) {
                // use
                for (Operand.VirtualReg useVreg : q.getUseVregList()) {
                    if (!block.livenessFlow.def.contains(useVreg)) {
                        block.livenessFlow.use.add(useVreg);
                    }
                }
                // def
                for (Operand.VirtualReg defVreg : q.getDefVregList()) {
                    if (!block.livenessFlow.use.contains(defVreg)) {
                        block.livenessFlow.def.add(defVreg);
                    }
                }
            }
        }

        // 计算 in 和 out
        while (true) {
            boolean changed = false;
            for (int i = blockList.size() - 1; i >= 0; i--) {
                Block block = blockList.get(i);
                if (block.next != null) block.livenessFlow.out.addAll(block.next.livenessFlow.in);
                if (block.jumpNext != null) block.livenessFlow.out.addAll(block.jumpNext.livenessFlow.in);
                // in = use ∪ (out – def)
                Set<Operand.VirtualReg> inAns = new HashSet<>(block.livenessFlow.out);
                inAns.removeAll(block.livenessFlow.def);
                inAns.addAll(block.livenessFlow.use);
                if (!inAns.equals(block.livenessFlow.in)) {
                    changed = true;
                    block.livenessFlow.in = inAns;
                }
            }
            if (!changed) break;
        }
    }
}

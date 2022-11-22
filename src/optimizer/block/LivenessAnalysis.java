package optimizer.block;

import intercode.Operand;
import intercode.Quaternion;

import java.util.HashSet;
import java.util.Set;

public class LivenessAnalysis {
    public static void doAnalysis(FuncBlocks funcBlocks) {
        // 计算所有 block 的 use 和 def
        for (Block block : funcBlocks.blockList) {
            block.use = new HashSet<>();
            block.def = new HashSet<>();
            block.in = new HashSet<>();
            block.out = new HashSet<>();

            for (Quaternion q : block.blockInter) {
                // use
                for (Operand.VirtualReg useVreg : q.getUseVregList()) {
                    if (!block.def.contains(useVreg)) {
                        block.use.add(useVreg);
                    }
                }
                // def
                for (Operand.VirtualReg defVreg : q.getDefVregList()) {
                    if (!block.use.contains(defVreg)) {
                        block.def.add(defVreg);
                    }
                }
            }
        }

        // 计算 in 和 out
        while (true) {
            boolean changed = false;
            for (int i = funcBlocks.blockList.size() - 1; i >= 0; i--) {
                Block block = funcBlocks.blockList.get(i);
                if (block.next != null) block.out.addAll(block.next.in);
                if (block.jumpNext != null) block.out.addAll(block.jumpNext.in);
                // in = use ∪ (out – def)
                Set<Operand.VirtualReg> inAns = new HashSet<>(block.out);
                inAns.removeAll(block.def);
                inAns.addAll(block.use);
                if (!inAns.equals(block.in)) {
                    changed = true;
                    block.in = inAns;
                }
            }
            if (!changed) break;
        }
    }
}

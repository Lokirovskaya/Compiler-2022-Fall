package optimizer.register;

import intercode.Operand.VirtualReg;
import intercode.Quaternion;
import optimizer.block.Block;
import optimizer.block.FuncBlocks;
import util.Pair;

import java.util.*;
import java.util.function.Function;

// 活跃变量分析
// 不计算全局变量和已分配寄存器的变量
class LivenessAnalysis {
    public static List<Interval> run(FuncBlocks funcBlocks) {
        Map<VirtualReg, Interval> vregIntervalMap = new HashMap<>();
        Function<VirtualReg, Interval> getInterval = vreg -> {
            if (vregIntervalMap.containsKey(vreg))
                return vregIntervalMap.get(vreg);
            else {
                Interval interval = new Interval();
                interval.vreg = vreg;
                vregIntervalMap.put(vreg, interval);
                return interval;
            }
        };

        // 计算所有 block 的 use 和 def
        for (Block block : funcBlocks.blockList) {
            block.use = new HashSet<>();
            block.def = new HashSet<>();
            block.in = new HashSet<>();
            block.out = new HashSet<>();

            for (Quaternion q : block.blockInter) {
                // use
                for (VirtualReg useVreg : q.getUseVregList()) {
                    if (!block.def.contains(useVreg) && needAlloc(useVreg)) {
                        block.use.add(useVreg);
                    }
                }
                // def
                for (VirtualReg defVreg : q.getDefVregList()) {
                    if (!block.use.contains(defVreg) && needAlloc(defVreg)) {
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
                Set<VirtualReg> inAns = new HashSet<>(block.out);
                inAns.removeAll(block.def);
                inAns.addAll(block.use);
                if (!inAns.equals(block.in)) {
                    changed = true;
                    block.in = inAns;
                }
            }
            if (!changed) break;
        }

        // 计算活跃区间
        for (int i = funcBlocks.blockList.size() - 1; i >= 0; i--) {
            Block block = funcBlocks.blockList.get(i);
            int blockStart = block.blockInter.get(0).id;
            int blockEnd = block.blockInter.get(block.blockInter.size() - 1).id;

            for (VirtualReg outVreg : block.out) {
                getInterval.apply(outVreg).addRange(blockStart, blockEnd + 1);
            }

            for (int j = block.blockInter.size() - 1; j >= 0; j--) {
                Quaternion quater = block.blockInter.get(j);
                // def
                for (VirtualReg defVreg : quater.getDefVregList()) {
                    if (needAlloc(defVreg)) {
                        List<Pair<Integer, Integer>> ranges = getInterval.apply(defVreg).rangeList;
                        if (ranges.size() > 0)
                            ranges.get(0).first = quater.id;
                        else {
                            if (quater.op != Quaternion.OperatorType.FUNC)
                                quater.isUselessAssign = true;
                        }
                    }
                }
                // use
                for (VirtualReg useVreg : quater.getUseVregList()) {
                    if (needAlloc(useVreg)) {
                        getInterval.apply(useVreg).addRange(blockStart, quater.id);
                        getInterval.apply(useVreg).addUsePoint(quater.id);
                    }
                }
            }
        }

        vregIntervalMap.values().forEach(interval -> interval.usePointList.sort(Comparator.comparingInt(x -> x)));
        return new ArrayList<>(vregIntervalMap.values());
    }

    private static boolean needAlloc(VirtualReg vreg) {
        return !vreg.isGlobal && vreg.realReg < 0;
    }
}

package optimizer.register;

import intercode.Operand.VirtualReg;
import intercode.Quaternion;
import optimizer.block.Block;
import optimizer.block.FuncBlocks;
import util.Pair;

import java.util.*;
import java.util.function.Function;

// 活跃变量分析，填充 block 的 in, out, use, def 字段
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

            block.blockInter.forEachItem(quater -> {
                // use
                for (VirtualReg useVreg : quater.getUseVregList()) {
                    if (!block.def.contains(useVreg) && !useVreg.isGlobal) {
                        block.use.add(useVreg);
                    }
                }
                // def
                for (VirtualReg defVreg : quater.getDefVregList()) {
                    if (!block.use.contains(defVreg) && !defVreg.isGlobal) {
                        block.def.add(defVreg);
                    }
                }
            });
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
            int blockStart = block.blockInter.getFirst().id;
            int blockEnd = block.blockInter.getLast().id;

            for (VirtualReg outVreg : block.out) {
                getInterval.apply(outVreg).addRange(blockStart, blockEnd);
            }

            block.blockInter.forEachItemReverse(quater -> {
                // def
                for (VirtualReg defVreg : quater.getDefVregList()) {
                    if (!defVreg.isGlobal) {
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
                    if (!useVreg.isGlobal) {
                        getInterval.apply(useVreg).addRange(blockStart, quater.id);
                        getInterval.apply(useVreg).addUsePoint(quater.id);
                    }
                }
            });
        }

        vregIntervalMap.values().forEach(interval -> interval.usePointList.sort(Comparator.comparingInt(x -> x)));
        return new ArrayList<>(vregIntervalMap.values());
    }
}

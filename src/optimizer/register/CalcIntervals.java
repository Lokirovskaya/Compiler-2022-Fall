package optimizer.register;

import intercode.Operand.VirtualReg;
import intercode.Quaternion;
import optimizer.block.Block;
import optimizer.block.FuncBlocks;
import optimizer.block.LivenessAnalysis;
import util.Pair;

import java.util.*;
import java.util.function.Function;

// 活跃变量分析
// 不计算全局变量和已分配寄存器的变量
class CalcIntervals {
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

        LivenessAnalysis.doAnalysis(funcBlocks);

        for (int i = funcBlocks.blockList.size() - 1; i >= 0; i--) {
            Block block = funcBlocks.blockList.get(i);
            int blockStart = block.blockInter.get(0).id;
            int blockEnd = block.blockInter.get(block.blockInter.size() - 1).id;

            for (VirtualReg outVreg : block.livenessFlow.out) {
                getInterval.apply(outVreg).addRange(blockStart, blockEnd + 1);
            }

            for (int j = block.blockInter.size() - 1; j >= 0; j--) {
                Quaternion quater = block.blockInter.get(j);
                // def
                for (VirtualReg defVreg : quater.getDefVregList()) {
                    if (needAlloc(defVreg)) {
                        List<Pair<Integer, Integer>> ranges = getInterval.apply(defVreg).rangeList;
                        if (ranges.size() > 0) {
                            // 使得 def 尽量靠后
                            if (ranges.get(0).first <= quater.id)
                                ranges.get(0).first = quater.id;
                                // 基本块内更早的 def，标记为无用
                            else quater.isUselessAssign = true;
                        }
                        else {
                            // 如果当前基本块内没有 use，标记 def 为无用
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

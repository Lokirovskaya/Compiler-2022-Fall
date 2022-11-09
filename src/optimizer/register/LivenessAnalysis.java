package optimizer.register;

import intercode.Operand.VirtualReg;
import optimizer.block.Block;
import optimizer.block.FuncBlocks;

import java.util.*;

// 活跃变量分析，填充 block 的 in, out, use, def 字段
public class LivenessAnalysis {
    private static final Map<VirtualReg, List<LiveRange>> vregRangesOfVregMap = new HashMap<>();

    public static Map<VirtualReg, List<LiveRange>> run(FuncBlocks funcBlocks) {
        vregRangesOfVregMap.clear();
        // hashMap = {
        //     vreg1: [range11, range12, ...],
        //     vreg2: [range21, range22, ...],
        //     ...
        // }

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
                addRange(new LiveRange(outVreg, blockStart, blockEnd));
            }

            block.blockInter.forEachItemReverse(quater -> {
                // def
                for (VirtualReg defVreg : quater.getDefVregList()) {
                    List<LiveRange> ranges = vregRangesOfVregMap.get(defVreg);
                    if (ranges != null)
                        ranges.get(0).start = quater.id;
                }

                // use
                for (VirtualReg useVreg: quater.getUseVregList()) {
                    if (!useVreg.isGlobal) {
                        addRange(new LiveRange(useVreg, blockStart, quater.id));
                        addUsePoint(useVreg, quater.id);
                    }
                }
            });
        }
        vregRangesOfVregMap.values().forEach(rangesOfVreg -> rangesOfVreg.forEach(range -> range.usePointList.sort(Comparator.comparingInt(x -> x))));
        return vregRangesOfVregMap;
    }

    private static void addRange(LiveRange range) {
        if (!vregRangesOfVregMap.containsKey(range.vreg))
            vregRangesOfVregMap.put(range.vreg, new ArrayList<>());
        vregRangesOfVregMap.get(range.vreg).add(range);
        mergeLiveRanges(vregRangesOfVregMap.get(range.vreg));
    }

    private static void addUsePoint(VirtualReg vreg, int usePoint) {
        vregRangesOfVregMap.get(vreg).get(0).usePointList.add(usePoint);
    }

    private static void mergeLiveRanges(List<LiveRange> rangeList) {
        rangeList.sort(Comparator.comparingInt(x -> x.start));
        int nowIdx = 0, nextIdx = 1;
        while (nextIdx < rangeList.size()) {
            LiveRange now = rangeList.get(nowIdx), next = rangeList.get(nextIdx);
            if (next.start <= now.end + 1) {
                now.end = Math.max(now.end, next.end);
                next.start = -1; // to be deleted
                nextIdx++;
                continue;
            }
            nowIdx = nextIdx;
            nextIdx++;
        }
        // rangeList[0] 一定不会被移除
        assert rangeList.get(0).start != -1;
        for (int i = 1; i < rangeList.size(); i++) {
            if (rangeList.get(i).start == -1) {
                rangeList.get(i - 1).usePointList.addAll(rangeList.get(i).usePointList);
                rangeList.remove(i);
                i--;
            }
        }
    }
}

package optimizer.block;

import intercode.Operand;
import intercode.Operand.VirtualReg;

import java.util.*;
import java.util.function.BiConsumer;

import static intercode.Quaternion.OperatorType.SET_ARRAY;

// 活跃变量分析，填充 block 的 in, out, use, def 字段
public class LivenessAnalysis {
    public static Map<VirtualReg, List<LiveRange>> run(FuncBlocks funcBlocks) {
        // hashMap = {
        //     vreg1: [range11, range12, ...],
        //     vreg2: [range21, range22, ...],
        //     ...
        // }
        Map<VirtualReg, List<LiveRange>> vregRangesOfVregMap = new HashMap<>();

        BiConsumer<VirtualReg, LiveRange> addRange = (vreg, range) -> {
            if (!vregRangesOfVregMap.containsKey(vreg))
                vregRangesOfVregMap.put(vreg, new ArrayList<>());
            vregRangesOfVregMap.get(vreg).add(range);
        };
        BiConsumer<VirtualReg, Integer> addUsePoint = (vreg, usePoint) ->
                vregRangesOfVregMap.get(vreg)
                        .get(vregRangesOfVregMap.get(vreg).size() - 1)
                        .usePointList.add(usePoint);


        // 计算所有 block 的 use 和 def
        for (Block block : funcBlocks.blockList) {
            block.use = new HashSet<>();
            block.def = new HashSet<>();
            block.in = new HashSet<>();
            block.out = new HashSet<>();

            block.blockInter.forEachItem(quater -> {
                VirtualReg target = quater.target;
                Operand x1 = quater.x1, x2 = quater.x2;
                if (x1 instanceof VirtualReg && !block.def.contains(x1) && !((VirtualReg) x1).isGlobal)
                    block.use.add((VirtualReg) x1);
                if (x2 instanceof VirtualReg && !block.def.contains(x2) && !((VirtualReg) x2).isGlobal)
                    block.use.add((VirtualReg) x2);
                // SET_ARRAY 的 @t 是假的 target，它应当是 use 而非 def
                if (quater.op == SET_ARRAY && !block.def.contains(target) && !target.isGlobal)
                    block.use.add(target);

                if (target != null && quater.op != SET_ARRAY && !block.use.contains(target) && !target.isGlobal)
                    block.def.add(target);

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
                addRange.accept(outVreg, new LiveRange(outVreg, blockStart, blockEnd));
            }

            block.blockInter.forEachItemReverse(quater -> {
                VirtualReg target = quater.target;
                Operand x1 = quater.x1, x2 = quater.x2;

                if (target != null && quater.op != SET_ARRAY) {
                    List<LiveRange> ranges = vregRangesOfVregMap.get(target);
                    if (ranges != null)
                        ranges.get(ranges.size() - 1).start = quater.id;
                }

                if (quater.op == SET_ARRAY && target != null && !target.isGlobal) {
                    addRange.accept(target, new LiveRange(target, blockStart, quater.id));
                    addUsePoint.accept(target, quater.id);
                }
                if (x1 instanceof VirtualReg && !((VirtualReg) x1).isGlobal) {
                    addRange.accept((VirtualReg) x1, new LiveRange((VirtualReg) x1, blockStart, quater.id));
                    addUsePoint.accept((VirtualReg) x1, quater.id);
                }
                if (x2 instanceof VirtualReg && !((VirtualReg) x2).isGlobal) {
                    addRange.accept((VirtualReg) x2, new LiveRange((VirtualReg) x2, blockStart, quater.id));
                    addUsePoint.accept((VirtualReg) x2, quater.id);
                }


            });
        }
        vregRangesOfVregMap.values().forEach(rangesOfVreg -> mergeLiveRanges(rangesOfVreg));
        vregRangesOfVregMap.values().forEach(rangesOfVreg -> rangesOfVreg.forEach(range -> range.usePointList.sort(Comparator.comparingInt(x -> x))));
        return vregRangesOfVregMap;
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

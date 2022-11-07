package optimizer.block;

import intercode.Operand;
import intercode.Operand.VirtualReg;
import util.Pair;

import java.util.*;
import java.util.function.BiConsumer;
import java.util.stream.Collectors;

import static intercode.Quaternion.OperatorType.SET_ARRAY;

// 活跃变量分析，填充 block 的 in, out, use, def 字段
public class LivenessAnalysis {
    // 返回 {
    //     vreg1: [range1, range2, ...],
    //     vreg2: [range1, range2, ...],
    //     ...
    // }
    public static Map<VirtualReg, List<LiveRange>> run(FuncBlocks funcBlocks) {
        Map<VirtualReg, List<LiveRange>> vregLiveRangesMap = new HashMap<>();

        BiConsumer<VirtualReg, LiveRange> addRange = (vreg, range) -> {
            if (!vregLiveRangesMap.containsKey(vreg))
                vregLiveRangesMap.put(vreg, new ArrayList<>());
            vregLiveRangesMap.get(vreg).add(range);
        };

        // 计算所有 block 的 use 和 def
        for (Block block : funcBlocks.blockList) {
            block.use = new HashSet<>();
            block.def = new HashSet<>();
            block.in = new HashSet<>();
            block.out = new HashSet<>();

            block.blockInter.forEachItem(quater -> {
                VirtualReg target = quater.target;
                Operand x1 = quater.x1, x2 = quater.x2;
                if (x1 instanceof VirtualReg && !block.def.contains(x1))
                    block.use.add((VirtualReg) x1);
                if (x2 instanceof VirtualReg && !block.def.contains(x2))
                    block.use.add((VirtualReg) x2);
                // SET_ARRAY 的 @t 是假的 target，它应当是 use 而非 def
                if (quater.op == SET_ARRAY && !block.def.contains(target))
                    block.use.add(target);

                if (target != null && quater.op != SET_ARRAY && !block.use.contains(target))
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

                if (quater.op == SET_ARRAY)
                    addRange.accept(target, new LiveRange(target, blockStart, quater.id));
                if (x1 instanceof VirtualReg)
                    addRange.accept((VirtualReg) x1, new LiveRange((VirtualReg) x1, blockStart, quater.id));
                if (x2 instanceof VirtualReg)
                    addRange.accept((VirtualReg) x2, new LiveRange((VirtualReg) x2, blockStart, quater.id));

                if (target != null && quater.op != SET_ARRAY) {
                    List<LiveRange> ranges = vregLiveRangesMap.get(target);
                    // 若不满足此 if，说明 target 从未被使用
                    if (ranges != null) {
                        ranges.get(ranges.size() - 1).start = quater.id;
                    }
                }
            });
        }
        vregLiveRangesMap.values().forEach(liveRangeList -> mergeLiveRanges(liveRangeList));
        return vregLiveRangesMap;
    }

    private static void mergeLiveRanges(List<LiveRange> liveRangeList) {
        liveRangeList.sort(Comparator.comparingInt(x -> x.start));
        int nowIdx = 0, nextIdx = 1;
        while (nextIdx < liveRangeList.size()) {
            LiveRange now = liveRangeList.get(nowIdx), next = liveRangeList.get(nextIdx);
            if (next.start <= now.end + 1) {
                now.end = Math.max(now.end, next.end);
                next.start = -1; // to be deleted
                nextIdx++;
                continue;
            }
            nowIdx = nextIdx;
            nextIdx++;
        }
        for (int i = 0; i < liveRangeList.size(); i++) {
            if (liveRangeList.get(i).start == -1) {
                liveRangeList.remove(i);
                i--;
            }
        }
    }

    public static void printLiveRanges(Map<VirtualReg, List<Pair<Integer, Integer>>> vregLiveRangesMap) {
        for (VirtualReg vreg : vregLiveRangesMap.keySet().stream()
                .sorted(Comparator.comparingInt(x -> x.regID))
                .collect(Collectors.toList())) {
            System.out.printf("%s: ", vreg);
            System.out.printf("%s\n", vregLiveRangesMap.get(vreg));
        }
        System.out.println();
    }
}

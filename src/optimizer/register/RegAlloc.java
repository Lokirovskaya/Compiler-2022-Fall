package optimizer.register;

import intercode.InterCode;
import intercode.Operand.VirtualReg;
import optimizer.block.*;
import util.Wrap;

import java.util.*;

public class RegAlloc {
    public static void run(InterCode inter) {
        // 为中间代码编号
        Wrap<Integer> id = new Wrap<>(1);
        inter.forEachItem(quater -> {
            quater.id = id.get();
            id.set(id.get() + 1);
        });

        for (FuncBlocks funcBlocks : SplitBlock.split(inter)) {
            Map<VirtualReg, List<LiveRange>> vregRangesOfVregMap = LivenessAnalysis.run(funcBlocks);
            // 已分配寄存器的活跃区间集合，不需要有序
            Set<LiveRange> active = new HashSet<>();
            // 未活跃的区间集合，需要按开始时间升序
            List<LiveRange> unhandled = new ArrayList<>();
            // 分配的结果
            List<LiveRange> allocResult = new ArrayList<>();

            // 初始化 unhandled
            vregRangesOfVregMap.values().forEach(rangesOfVreg -> unhandled.addAll(rangesOfVreg));
            unhandled.sort(Comparator.comparingInt(range -> range.start));

            RegPool pool = new RegPool();
            for (LiveRange curRange : unhandled) {
                // 寻找 curRange.start 时已经死去的 range，回收它的寄存器，将其移出 active，进入 result
                active.removeIf(range -> {
                    if (range.end < curRange.start) {
                        pool.free(range.realReg);
                        allocResult.add(range);
                    }
                    return range.end < curRange.start;
                });

                // 参数的分配：$a0 ~ $a3
                if (curRange.vreg.isParam) {
                    Integer reg = pool.getParam();
                    if (reg != null) {
                        curRange.realReg = reg;
                        active.add(curRange);
                    }
                    else {
                        // 溢出使用次数最少的参数
                        LiveRange rangeToBeSpill = active.stream()
                                .filter(r -> r.vreg.isParam)
                                .min(Comparator.comparingInt(r -> r.usePointList.size()))
                                .orElse(null);
                        assert rangeToBeSpill != null;
                        curRange.realReg = rangeToBeSpill.realReg;
                        active.add(curRange);
                        active.remove(rangeToBeSpill);
                    }
                }
                // 其它变量
                else {
                    Integer reg = pool.get();
                    if (reg != null) {
                        curRange.realReg = reg;
                        active.add(curRange);
                    }
                    else {
                        // 对所有的 active，以及自身，计算溢出权重
                        int selfSpillWeight;
                        int minActiveSpillWeight = Integer.MAX_VALUE;
                        LiveRange minActiveSpillRange = null;
                        // 溢出权重 = sum (curRange.start 之后的使用次数 * k)，todo: 循环中 k 取 3
                        selfSpillWeight = curRange.usePointList.size();
                        for (LiveRange r : active) {
                            int w = (int) r.usePointList.stream()
                                    .filter(use -> use >= curRange.start)
                                    .count();
                            if (w < minActiveSpillWeight) {
                                minActiveSpillWeight = w;
                                minActiveSpillRange = r;
                            }
                        }
                        // 溢出权重最小的 reg。如果 self 权重最小，什么都不做
                        if (selfSpillWeight > minActiveSpillWeight) {
                            curRange.realReg = minActiveSpillRange.realReg;
                            active.add(curRange);
                            active.remove(minActiveSpillRange);
                        }
                    }
                }
            }
            allocResult.addAll(active);

            for (LiveRange range : allocResult) {
                if (range.vreg.regRangeList == null)
                    range.vreg.regRangeList = new ArrayList<>();
                range.vreg.regRangeList.add(range);
            }
            // 输出分配表
            System.out.println(allocResult);
        }
    }

    private static class RegPool {
        private final Deque<Integer> regPool = new ArrayDeque<>(Arrays.asList(
                8, 9, 10, 11, 12, 13, 14, 15, // t0-t7
                16, 17, 18, 19, 20, 21, 22, 23, // s0-s7
                3, 30 // v1, fp
        ));

        private final Deque<Integer> paramRegPool =
                new ArrayDeque<>(Arrays.asList(4, 5, 6, 7)); // a0-a3

        // 若返回 null，表示池已空
        Integer get() {
            return regPool.pollFirst();
        }

        Integer getParam() {
            return paramRegPool.pollFirst();
        }

        // 放回一个寄存器
        void free(int reg) {
            assert reg > 0;
            if (4 <= reg && reg <= 7) paramRegPool.addFirst(reg);
            else regPool.addFirst(reg);
        }
    }
}

package optimizer;

import intercode.InterCode;
import intercode.Operand.VirtualReg;
import optimizer.block.*;
import util.Pair;
import util.Wrap;

import java.util.*;

public class RegAlloc {
    static void run(InterCode inter) {
        // 为中间代码编号，此后，请勿再改动中间代码，即需保证 RegAlloc 是优化的最后一步
        Wrap<Integer> id = new Wrap<>(1);
        inter.forEachItem(quater -> {
            quater.id = id.get();
            id.set(id.get() + 1);
        });

        for (FuncBlocks funcBlocks : SplitBlock.split(inter)) {
            List<LiveRange> liveRangeList = getLiveRangeListOfFunc(LivenessAnalysis.run(funcBlocks));

            RegPool pool = new RegPool();
            for (LiveRange range : liveRangeList) {
                if (range.vreg.isGlobal) continue;
                // 寻找在 currentQuaterId 时已经过期的 LiveRange r，free 它们占用的 reg
                for (LiveRange r : liveRangeList) {
                    if (r.end < range.start && !r.freed) {
                        if (r.vreg.realReg != -1) {
                            pool.free(r.vreg.realReg);
                            r.freed = true;
                        }
                    }
                }
                // 分配 reg
                Integer reg = pool.get();
                if (reg != null) {
                    range.vreg.realReg = reg;
                }
                // 寻找已经分配 reg 了的，未被 free 的，end 最大的 range，抢夺它占用的 reg，如果它没有占用 reg，放弃分配
                else {
                    LiveRange rangeToBeSpill = liveRangeList.stream()
                            .filter(r -> r.vreg.realReg > 0)
                            .filter(r -> !r.freed)
                            .max(Comparator.comparingInt(r -> r.end))
                            .orElse(null);
                    if (rangeToBeSpill != null) {

                    }
                }
            }

            System.out.println(funcBlocks.funcName);
            System.out.println(liveRangeList);
        }
    }

    // 返回一个 func 下所有 vreg 的最早 start 和最晚 end 组成的 range 列表，按 start 排序
    private static List<LiveRange> getLiveRangeListOfFunc(Map<VirtualReg, List<LiveRange>> vregLiveRangesMap) {
        List<LiveRange> liveRangeList = new ArrayList<>();
        for (List<LiveRange> liveRangesOfOneVreg : vregLiveRangesMap.values()) {
            if (liveRangesOfOneVreg.size() == 1) liveRangeList.add(liveRangesOfOneVreg.get(0));
            else {
                int start = liveRangesOfOneVreg.get(0).start;
                int end = liveRangesOfOneVreg.get(liveRangesOfOneVreg.size() - 1).end;
                liveRangeList.add(new LiveRange(liveRangesOfOneVreg.get(0).vreg, start, end));
            }
        }
        liveRangeList.sort(Comparator.comparingInt(range -> range.start));
        return liveRangeList;
    }

    private static class RegPool {
        private final Deque<Integer> regPool = new ArrayDeque<>(Arrays.asList(
                5, 6, 7, // a1-a3
                8, 9, 10, 11, 12, 13, 14, 15, // t0-t7
                16, 17, 18, 19, 20, 21, 22, 23, // s0-s7
                3, 30 // v1, fp
        ));

        // 若返回 null，表示池已空
        Integer get() {
            if (regPool.isEmpty()) return null;
            else return regPool.pollFirst();
        }

        // 放回一个寄存器
        void free(int reg) {
            assert reg > 0;
            regPool.addFirst(reg);
        }
    }
}

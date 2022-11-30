package optimizer.register;

import intercode.Quaternion;
import optimizer.block.Block;
import optimizer.block.FuncBlocks;
import optimizer.block.SplitBlock;

import java.util.*;
import java.util.stream.Collectors;

import static intercode.Quaternion.OperatorType.*;

// 寄存器分配，顺便删除无用赋值
public class RegAlloc {
    private static final boolean DEBUG = false;

    public static void run(List<Quaternion> inter) {
        // 为中间代码编号
        // 同时查询函数的最大参数个数，决定是否释放 $a1~$a3 寄存器
        int id = 1;
        int maxParam = 0;
        for (Quaternion q : inter) {
            q.id = id++;
            if (q.op == FUNC) maxParam = Math.max(maxParam, q.list.size());
        }

        final int A1 = 5, A2 = 6, A3 = 7;
        if (maxParam == 0 || maxParam == 1) RegPool.fullPool.addAll(Arrays.asList(A1, A2, A3));
        if (maxParam == 2) RegPool.fullPool.addAll(Arrays.asList(A2, A3));
        if (maxParam == 3) RegPool.fullPool.add(A3);

        for (FuncBlocks funcBlocks : SplitBlock.split(inter)) {
            List<Interval> intervalList = CalcIntervals.run(funcBlocks);
            if (DEBUG) {
                System.out.println("\n========================");
                funcBlocks.printFuncBlocks();
            }

            // 已分配寄存器的活跃区间集合，不需要有序
            Set<Interval> active = new HashSet<>();
            // 未活跃的区间集合，需要按开始时间升序遍历
            List<Interval> unhandled = intervalList.stream()
                    .filter(interval -> interval.rangeList.size() > 0)
                    .sorted(Comparator.comparingInt(interval -> interval.start()))
                    .collect(Collectors.toList());
            // 分配结果的集合
            List<Interval> result = new ArrayList<>();

            RegPool pool = new RegPool();

            for (Interval curInterval : unhandled) {
                // 不要分配全局变量
                if (curInterval.vreg.isGlobal) continue;
                // 寻找 curInterval.start 时已经死去的 range（可以取等号），回收它的寄存器，将其移出 active，加入 result
                active.removeIf(interval -> {
                    if (interval.end() <= curInterval.start()) {
                        pool.free(interval.realReg);
                        result.add(interval);
                    }
                    return interval.end() <= curInterval.start();
                });

                Integer reg = pool.fetch();
                if (reg != null) {
                    curInterval.realReg = reg;
                    active.add(curInterval);
                }
                else {
                    // 对所有的 active，以及自身，计算溢出权重
                    int selfSpillWeight;
                    int minActiveSpillWeight = Integer.MAX_VALUE;
                    Interval intervalToSpill = null;
                    // 溢出权重 = sum (curInterval.start 之后的使用次数 * k)，todo: 循环中 k 取 3
                    selfSpillWeight = curInterval.usePointList.size();
                    for (Interval interval : active) {
                        int w = (int) interval.usePointList.stream()
                                .filter(use -> use >= curInterval.start())
                                .count();
                        if (w < minActiveSpillWeight) {
                            minActiveSpillWeight = w;
                            intervalToSpill = interval;
                        }
                    }
                    // 溢出权重最小的 reg。如果 self 权重最小，什么都不做
                    if (minActiveSpillWeight < selfSpillWeight) {
                        curInterval.realReg = intervalToSpill.realReg;
                        intervalToSpill.realReg = -1;
                        active.add(curInterval);
                        active.remove(intervalToSpill);
                    }
                }
            }
            result.addAll(active);

            // 填写 VirtualReg.realReg 字段
            result.forEach(interval -> interval.vreg.realReg = interval.realReg);
            // 填写 Quaternion.activeRegSet 字段
            for (Block block : funcBlocks.blockList) {
                block.blockInter.forEach(q -> {
                    if (q.op == CALL) {
                        q.activeRegSet = new HashSet<>();
                        for (Interval interval : result) {
                            if (interval.realReg >= 0 && interval.start() <= q.id && q.id < interval.end()) {
                                q.activeRegSet.add(interval.realReg);
                            }
                        }
                    }
                });
            }
            if (DEBUG) {
                System.out.println("reg alloc result:");
                System.out.println(result);
            }
        }

        // 删除无用赋值
        inter.removeIf(q -> q.isUselessAssign && q.op != GETINT);
    }
}

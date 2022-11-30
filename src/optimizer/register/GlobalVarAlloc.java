package optimizer.register;

import intercode.Operand.VirtualReg;
import intercode.Quaternion;
import util.Pair;

import java.util.*;
import java.util.stream.Collectors;

public class GlobalVarAlloc {
    public static void run(List<Quaternion> inter) {
        // 某个寄存器的引用计数
        Map<Integer, Integer> regRefCount = new HashMap<>();
        // 某个全局变量的引用计数
        Map<VirtualReg, Integer> gVarRefCount = new HashMap<>();

        for (Quaternion q : inter) {
            for (VirtualReg vreg : q.getAllVregList()) {
                if (vreg.realReg >= 0) {
                    regRefCount.putIfAbsent(vreg.realReg, 0);
                    regRefCount.computeIfPresent(vreg.realReg, (k, v) -> v + 1);
                }
                if (vreg.isGlobal) {
                    gVarRefCount.putIfAbsent(vreg, 0);
                    gVarRefCount.computeIfPresent(vreg, (k, v) -> v + 1);
                }
            }
        }

        // 还空余的寄存器
        Deque<Integer> freeRegList = new ArrayDeque<>(RegPool.fullPool);
        freeRegList.removeAll(regRefCount.keySet());

        // 根据使用数*权重，从大到小排序全局变量。[(gVar, Weight)] 数组
        List<Pair<VirtualReg, Double>> gVarList = new ArrayList<>();
        gVarRefCount.forEach((var, refCount) -> {
            double weight = refCount.doubleValue();
            if (var.isAddr) weight *= 1.5;
            gVarList.add(new Pair<>(var, weight));
        });
        gVarList.sort(Comparator.comparingDouble(pair -> -pair.second));

        // 全局变量按顺序分配空余寄存器
        for (Pair<VirtualReg, Double> gVarWeightPair : gVarList) {
            if (freeRegList.size() > 0)
                gVarWeightPair.first.realReg = freeRegList.pollFirst();
            else {
                // 已经没有空余寄存器可分配
            }
        }
    }
}

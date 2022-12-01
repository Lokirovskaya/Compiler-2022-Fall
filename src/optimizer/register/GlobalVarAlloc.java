package optimizer.register;

import intercode.Operand.VirtualReg;
import intercode.Quaternion;
import util.Pair;

import java.util.*;
import java.util.function.Consumer;

public class GlobalVarAlloc {
    public static void run(List<Quaternion> inter) {
        // 某个寄存器的引用计数
        Map<Integer, Integer> regRefCountMap = new HashMap<>();
        // 某个全局变量的引用计数
        Map<VirtualReg, Integer> gVarRefCountMap = new HashMap<>();
        // 某个寄存器分配到的所有 vreg
        Map<Integer, List<VirtualReg>> regVregListMap = new HashMap<>();

        for (Quaternion q : inter) {
            for (VirtualReg vreg : q.getAllVregList()) {
                if (vreg.realReg >= 0) {
                    regRefCountMap.putIfAbsent(vreg.realReg, 0);
                    regRefCountMap.computeIfPresent(vreg.realReg, (k, v) -> v + 1);
                    regVregListMap.putIfAbsent(vreg.realReg, new ArrayList<>());
                    regVregListMap.get(vreg.realReg).add(vreg);
                }
                if (vreg.isGlobal) {
                    gVarRefCountMap.putIfAbsent(vreg, 0);
                    gVarRefCountMap.computeIfPresent(vreg, (k, v) -> v + 1);
                }
            }
        }

        // List of (寄存器,引用计数)，按引用计数从小到大排序，未引用的 reg，引用计数为 0
        List<Pair<Integer, Integer>> regRefCountSorted = new ArrayList<>();
        for (int reg : RegPool.fullPool) {
            regRefCountSorted.add(new Pair<>(
                    reg,
                    regRefCountMap.getOrDefault(reg, 0)
            ));
        }
        regRefCountSorted.sort(Comparator.comparingInt(pair -> pair.second));
        // List of (gVreg,引用计数)，按引用计数从大到小排序
        List<Pair<VirtualReg, Integer>> gVarRefCountSorted = new ArrayList<>();
        gVarRefCountMap.forEach((var, refCount) -> gVarRefCountSorted.add(new Pair<>(var, refCount)));
        gVarRefCountSorted.sort(Comparator.comparingInt(pair -> (-pair.second)));
        // lambda: 取消所有 reg 的分配
        Consumer<Integer> removeAllocatedReg = reg -> {
            if (regVregListMap.containsKey(reg)) {
                regVregListMap.get(reg).forEach(vreg -> vreg.realReg = -1);
            }
        };

        for (int i = 0; i < regRefCountSorted.size() && i < gVarRefCountSorted.size(); i++) {
            // 只有全局变量的引用计数 > reg 的引用计数时，才会分配
            if (gVarRefCountSorted.get(i).second > regRefCountSorted.get(i).second) {
                VirtualReg gVar = gVarRefCountSorted.get(i).first;
                int reg = regRefCountSorted.get(i).first;
                removeAllocatedReg.accept(reg);
                gVar.realReg = reg;
            }
        }
    }
}

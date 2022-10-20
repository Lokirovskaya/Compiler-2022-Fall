package mips;

import intercode.InterCode;
import intercode.Operand;
import intercode.Operand.VirtualReg;
import util.Pair;
import util.Wrap;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Consumer;

import static intercode.Quaternion.OperatorType.*;

class Allocator {
    // return:
    // pair.first: map of VR-offset,
    // pair.second: map of func-size
    static Pair<Map<VirtualReg, Integer>, Map<String, Integer>> alloc(InterCode inter) {
        Map<VirtualReg, Integer> vregOffsetMap = new HashMap<>();
        Map<String, Integer> funcSizeMap = new HashMap<>();
        Wrap<String> curFunc = new Wrap<>(null);
        Wrap<Integer> curOffset = new Wrap<>(-4); // 类似于栈的 top 初值为 -1
        Consumer<VirtualReg> tryAllocOneReg = reg -> { // 尝试为一个 reg 分配空间，分配过了就算了
            if (!vregOffsetMap.containsKey(reg)) {
                curOffset.set(curOffset.get() + 4);
                vregOffsetMap.put(reg, curOffset.get());
                System.out.println("Alloc for reg " + reg + " at " + curOffset.get());
            }
        };

        inter.forEach(p -> {
            // todo 全局变量区
            if (curFunc.get() == null && p.get().op != FUNC) {
                return;
            }
            // 遇到下一个 func，结算当前 func
            if (p.get().op == FUNC) {
                if (curFunc.get() == null) curFunc.set(p.get().label.name);
                else {
                    // size 应当 == offset + 4，但是还要多存一个 $ra
                    funcSizeMap.put(curFunc.get(), curOffset.get() + 8);
                    System.out.println("Func " + curFunc.get() + " alloc " + (curOffset.get() + 8));
                    curFunc.set(p.get().label.name);
                    curOffset.set(-4);
                }
                return;
            }
            // todo: 数组 alloc
            // 局部 reg 的 alloc，跳过立即数、全局 reg 和已分配寄存器的 reg
            Operand[] regs = new Operand[]{p.get().target, p.get().x1, p.get().x2};
            for (Operand reg : regs) {
                if (reg instanceof VirtualReg && !((VirtualReg) reg).isGlobal && ((VirtualReg) reg).realReg < 0) {
                    tryAllocOneReg.accept((VirtualReg) reg);
                }
            }
        });
        funcSizeMap.put(curFunc.get(), curOffset.get() + 8);
        System.out.println("Func " + curFunc.get() + " alloc " + (curOffset.get() + 8));
        return new Pair<>(vregOffsetMap, funcSizeMap);
    }
}

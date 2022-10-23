package mips;

import intercode.InterCode;
import intercode.Operand;
import intercode.Operand.VirtualReg;
import util.Wrap;

import static intercode.Quaternion.OperatorType.*;

class Allocator {
    static AllocationInfo alloc(InterCode inter) {
        AllocationInfo allocInfo = new AllocationInfo();

        Wrap<AllocationInfo.FunctionInfo> curFuncInfo = new Wrap<>(null);
        Wrap<Integer> curOffset = new Wrap<>(0); // 0($sp) 的位置保留给 $ra


        inter.forEach(p -> {
            // 全局变量区
            if (curFuncInfo.get() == null && p.get().op != FUNC) {

                return;
            }

            // 函数区
            // 遇到下一个 func，结算当前 func
            if (p.get().op == FUNC) {
                if (curFuncInfo.get() != null) {
                    curFuncInfo.get().size = curOffset.get() + 4;
                    allocInfo.funcMap.put(curFuncInfo.get().name, curFuncInfo.get());
                    curOffset.set(0);
                }
                curFuncInfo.set(new AllocationInfo.FunctionInfo());
                curFuncInfo.get().name = p.get().label.name;
                return;
            }
            // todo: 数组 alloc
            // 对任意四元式中 vreg 的分配，跳过立即数、全局 vreg 和已分配寄存器的 vreg
            for (Operand reg : new Operand[]{p.get().target, p.get().x1, p.get().x2}) {
                if (reg instanceof VirtualReg && !((VirtualReg) reg).isGlobal && ((VirtualReg) reg).realReg < 0) {
                    if (!allocInfo.vregOffsetMap.containsKey(reg)) {
                        curOffset.set(curOffset.get() + 4);
                        allocInfo.vregOffsetMap.put((VirtualReg) reg, curOffset.get());
                    }
                }
            }
            // 记录参数对应的 vreg
            if (p.get().op == PARAM || p.get().op == PARAM_ARRAY) {
                curFuncInfo.get().paramList.add(p.get().target);
            }
        });
        curFuncInfo.get().size = curOffset.get() + 4;
        allocInfo.funcMap.put(curFuncInfo.get().name, curFuncInfo.get());
        return allocInfo;
    }
}

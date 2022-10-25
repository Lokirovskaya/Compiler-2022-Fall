package mips;

import intercode.InterCode;
import intercode.Operand;
import intercode.Operand.InstNumber;
import intercode.Operand.VirtualReg;
import util.Wrap;

import static intercode.Quaternion.OperatorType.*;

class Allocator {
    static AllocationInfo alloc(InterCode inter) {
        AllocationInfo allocInfo = new AllocationInfo();

        Wrap<AllocationInfo.FunctionInfo> curFuncInfo = new Wrap<>(null);
        Wrap<Integer> curOffset = new Wrap<>(0); // 0($sp) 的位置保留给 $ra


        inter.forEach(p -> {
            // 函数区
            // 遇到下一个 func，结算当前 func
            if (p.get().op == FUNC) {
                if (curFuncInfo.get() != null) {
                    curFuncInfo.get().size = curOffset.get() + 4;
                    allocInfo.funcMap.put(curFuncInfo.get().name, curFuncInfo.get());
                }
                curFuncInfo.set(new AllocationInfo.FunctionInfo());
                curFuncInfo.get().name = p.get().label.name;
                curOffset.set(0);
                return;
            }

            // 局部变量区
            // 对任意四元式中 vreg 的分配，跳过立即数、全局 vreg 和已分配寄存器的 vreg
            for (Operand reg : new Operand[]{p.get().target, p.get().x1, p.get().x2}) {
                if (reg instanceof VirtualReg && ((VirtualReg) reg).realReg < 0) {
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
            // 分配数组空间，位置紧邻数组地址
            else if (p.get().op == ALLOC) {
                assert p.get().x1 instanceof InstNumber;
                int arraySize = ((InstNumber) p.get().x1).number * 4;
                curOffset.set(curOffset.get() + arraySize);
            }
        });
        curFuncInfo.get().size = curOffset.get() + 4;
        allocInfo.funcMap.put(curFuncInfo.get().name, curFuncInfo.get());
        return allocInfo;
    }
}

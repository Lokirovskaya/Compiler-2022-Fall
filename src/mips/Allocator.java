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
        Wrap<Integer> curGlobalOffset = new Wrap<>(-4); // $gp 空间，不需要留 $ra
        inter.forEachItem(quater -> {
            // 函数
            // 遇到下一个 func，结算当前 func
            if (quater.op == FUNC) {
                if (curFuncInfo.get() != null) {
                    curFuncInfo.get().size = curOffset.get() + 4;
                    allocInfo.funcMap.put(curFuncInfo.get().name, curFuncInfo.get());
                }
                curFuncInfo.set(new AllocationInfo.FunctionInfo());
                curFuncInfo.get().name = quater.label.name;
                curOffset.set(0);
                return;
            }

            // 变量
            // 对任意四元式中 vreg 的分配，跳过立即数和已分配寄存器的 vreg
            for (Operand reg : new Operand[]{quater.target, quater.x1, quater.x2}) {
                if (reg instanceof VirtualReg && ((VirtualReg) reg).getRealReg(quater.id) < 0) {
                    if (((VirtualReg) reg).stackOffset < 0) {
                        if (((VirtualReg) reg).isGlobal) {
                            curGlobalOffset.set(curGlobalOffset.get() + 4);
                            ((VirtualReg) reg).stackOffset = curGlobalOffset.get();
                        }
                        else {
                            curOffset.set(curOffset.get() + 4);
                            ((VirtualReg) reg).stackOffset = curOffset.get();
                        }
                    }
                }
            }
            // 记录参数对应的 vreg
            if (quater.op == PARAM) {
                curFuncInfo.get().paramList.add(quater.target);
            }
            // 分配数组空间，位置紧邻数组地址
            else if (quater.op == ALLOC) {
                assert quater.x1 instanceof InstNumber;
                int arraySize = ((InstNumber) quater.x1).number * 4;
                if (quater.target.isGlobal) {
                    quater.target.stackOffset = curGlobalOffset.get();
                    curGlobalOffset.set(curGlobalOffset.get() + arraySize);
                }
                else {
                    quater.target.stackOffset = curOffset.get();
                    curOffset.set(curOffset.get() + arraySize);
                }
            }
        });
        curFuncInfo.get().size = curOffset.get() + 4;
        allocInfo.funcMap.put(curFuncInfo.get().name, curFuncInfo.get());
        return allocInfo;
    }
}

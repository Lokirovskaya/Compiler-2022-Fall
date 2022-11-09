package mips;

import intercode.InterCode;
import intercode.Operand.InstNumber;
import intercode.Operand.VirtualReg;
import util.Wrap;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static intercode.Quaternion.OperatorType.*;

class Allocator {
    // 1. 为栈上分配变量和寄存器的保存分配栈空间
    // 2. 获取所有函数的信息
    static Map<String, FunctionInfo> alloc(InterCode inter) {
        Map<String, FunctionInfo> funcInfoMap = new HashMap<>();

        Wrap<FunctionInfo> curFuncInfo = new Wrap<>(new FunctionInfo());
        curFuncInfo.get().name = ".global";
        Wrap<Integer> curOffset = new Wrap<>(0); // 0($sp) 的位置保留给 $ra
        Wrap<Integer> curGlobalOffset = new Wrap<>(-4); // $gp 空间，不需要留 $ra
        inter.forEachItem(quater -> {
            // 函数
            // 遇到下一个 func，结算当前 func
            if (quater.op == FUNC) {
                // 结算旧函数
                curFuncInfo.get().frameSize = curOffset.get() + 4;
                funcInfoMap.put(curFuncInfo.get().name, curFuncInfo.get());
                // 新函数
                curFuncInfo.set(new FunctionInfo());
                curFuncInfo.get().name = quater.label.name;
                // 预留参数的位置
                int stackParamCount = quater.list.size() > 4 ? quater.list.size() - 4 : 0;
                curOffset.set(stackParamCount * 4);
            }

            // 变量
            // 对任意四元式中 vreg 的分配，跳过立即数和已分配寄存器的 vreg
            List<VirtualReg> allVreg = quater.getUseVregList();
            allVreg.addAll(quater.getDefVregList());
            for (VirtualReg vreg : allVreg) {
                // 如果一个 vreg 存在未分配寄存器的使用，就为它分配栈空间
                if (vreg.getRealReg(quater.id) < 0) {
                    if (vreg.stackOffset < 0) {
                        if (vreg.isGlobal) {
                            curGlobalOffset.set(curGlobalOffset.get() + 4);
                            vreg.stackOffset = curGlobalOffset.get();
                        }
                        else {
                            curOffset.set(curOffset.get() + 4);
                            vreg.stackOffset = curOffset.get();
                        }
                    }
                }
                // 否则，记录寄存器到函数的寄存器表中
                else {
                    curFuncInfo.get().regUseList.add(vreg.getRealReg(quater.id));
                }
            }
            // 分配数组空间，位置紧邻数组地址
            if (quater.op == ALLOC) {
                assert quater.x1 instanceof InstNumber;
                int arraySize = ((InstNumber) quater.x1).number * 4;
                if (quater.target.isGlobal) {
                    curGlobalOffset.set(curGlobalOffset.get() + 4);
                    quater.target.stackOffset = curGlobalOffset.get();
                    curGlobalOffset.set(curGlobalOffset.get() + arraySize);
                }
                else {
                    curOffset.set(curOffset.get() + 4);
                    quater.target.stackOffset = curOffset.get();
                    curOffset.set(curOffset.get() + arraySize);
                }
            }
            // 函数信息
            // 记录参数对应的 vreg
            else if (quater.op == CALL)
                curFuncInfo.get().hasCall = true;
            else if (quater.op == FUNC)
                curFuncInfo.get().paramList = quater.list.stream().map(o -> (VirtualReg) o).collect(Collectors.toList());
        });
        curFuncInfo.get().frameSize = curOffset.get() + 4;
        funcInfoMap.put(curFuncInfo.get().name, curFuncInfo.get());
        return funcInfoMap;
    }
}

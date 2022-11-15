package mips;

import intercode.Operand.InstNumber;
import intercode.Operand.VirtualReg;
import intercode.Quaternion;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static intercode.Quaternion.OperatorType.*;

class Allocator {
    // 1. 为栈上分配变量和寄存器的保存分配栈空间
    // 2. 获取所有函数的信息
    static Map<String, FunctionInfo> alloc(List<Quaternion> inter) {
        Map<String, FunctionInfo> funcInfoMap = new HashMap<>();

        FunctionInfo curFuncInfo = new FunctionInfo();
        curFuncInfo.name = ".global";
        int curOffset = 0; // 0($sp) 的位置保留给 $ra
        int curGlobalOffset = -4; // $gp 空间，不需要留 $ra

        for (Quaternion q : inter) {
            // 函数
            // 遇到下一个 func，结算当前 func
            if (q.op == FUNC) {
                // 结算旧函数
                curFuncInfo.frameSize = curOffset + 4;
                funcInfoMap.put(curFuncInfo.name, curFuncInfo);
                // 新函数
                curFuncInfo = new FunctionInfo();
                curFuncInfo.name = q.label.name;
                // 预留参数的位置
                int stackParamCount = q.list.size() > 4 ? q.list.size() - 4 : 0;
                curOffset = stackParamCount * 4;
            }

            // 变量
            // 对任意四元式中 vreg 的分配，跳过立即数和已分配寄存器的 vreg
            for (VirtualReg vreg : q.getAllVregList()) {
                // 如果一个 vreg 未分配寄存器，就为它分配栈空间
                if (vreg.realReg < 0) {
                    if (vreg.stackOffset < 0) {
                        if (vreg.isGlobal) {
                            curGlobalOffset += 4;
                            vreg.stackOffset = curGlobalOffset;
                        }
                        else {
                            curOffset += 4;
                            vreg.stackOffset = curOffset;
                        }
                    }
                }
                // 否则，记录寄存器到函数的寄存器表中
                else {
                    curFuncInfo.regUseSet.add(vreg.realReg);
                }
            }
            // 分配数组空间，位置紧邻数组地址
            if (q.op == ALLOC) {
                assert q.x1 instanceof InstNumber;
                int arraySize = ((InstNumber) q.x1).number * 4;
                if (q.target.isGlobal) {
                    curGlobalOffset += 4;
                    q.target.stackOffset = curGlobalOffset;
                    curGlobalOffset += arraySize;
                }
                else {
                    curOffset += 4;
                    q.target.stackOffset = curOffset;
                    curOffset += arraySize;
                }
            }
            // 函数信息
            // 记录参数对应的 vreg
            else if (q.op == CALL)
                curFuncInfo.isPureFunc = false;
            else if (q.op == FUNC)
                curFuncInfo.paramList = q.list.stream().map(o -> (VirtualReg) o).collect(Collectors.toList());
        }
        curFuncInfo.frameSize = curOffset + 4;
        funcInfoMap.put(curFuncInfo.name, curFuncInfo);
        return funcInfoMap;
    }
}

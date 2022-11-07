package mips;

import intercode.Operand.VirtualReg;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

class AllocationInfo {
    Map<String, FunctionInfo> funcMap = new HashMap<>(); // 每个 func 的信息

    public static class FunctionInfo {
        int size = 0; // func 调用栈的大小
        String name;
        List<VirtualReg> paramList = new ArrayList<>(); // func 的所有参数对应的 vreg
    }

    int getFuncSize(String funcName) {
        return funcMap.get(funcName).size;
    }

    VirtualReg getFuncParam(String funcName, int idx) {
        return funcMap.get(funcName).paramList.get(idx);
    }

    int getFuncParamCount(String funcName) {
        return funcMap.get(funcName).paramList.size();
    }

    void printAllocInfo() {
        for (String funcName : funcMap.keySet()) {
            FunctionInfo funcInfo = funcMap.get(funcName);
            System.out.printf("func %s, size %d\n", funcName, funcInfo.size);
            for (VirtualReg param : funcInfo.paramList) {
                System.out.printf("  param %s: %s\n", param.name, param.stackOffset);
            }
        }
    }
}

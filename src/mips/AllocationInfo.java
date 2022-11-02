package mips;

import intercode.Operand.VirtualReg;

import java.util.*;
import java.util.stream.Collectors;

class AllocationInfo {
    Map<VirtualReg, Integer> vregOffsetMap = new HashMap<>(); // 所有 vreg 在自身函数栈空间中的偏移；全局 vreg 则是相对于 $gp 的偏移
    Map<String, FunctionInfo> funcMap = new HashMap<>(); // 每个 func 的信息

    public static class FunctionInfo {
        int size = 0; // func 调用栈的大小
        String name;
        List<VirtualReg> paramList = new ArrayList<>(); // func 的所有参数对应的 vreg
    }

    Integer getVregOffset(VirtualReg vreg) {
        return vregOffsetMap.getOrDefault(vreg, null);
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
        for (VirtualReg reg : vregOffsetMap.keySet().stream().sorted(Comparator.comparingInt(x -> x.regID)).collect(Collectors.toList())) {
            System.out.printf("%s: %d\n", reg.toString(), vregOffsetMap.get(reg));
        }
        for (String funcName : funcMap.keySet()) {
            FunctionInfo funcInfo = funcMap.get(funcName);
            System.out.printf("func %s, size %d\n", funcName, funcInfo.size);
            for (VirtualReg param : funcInfo.paramList) {
                System.out.printf("  param %s: %s\n", param.name, vregOffsetMap.get(param).toString());
            }
        }
    }
}

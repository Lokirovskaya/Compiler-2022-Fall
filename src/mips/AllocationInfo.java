package mips;

import intercode.Operand.VirtualReg;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

class AllocationInfo {
    Map<VirtualReg, Integer> vregOffsetMap = new HashMap<>();
    Map<String, FunctionInfo> funcMap = new HashMap<>();

    public static class FunctionInfo {
        int size = 0;
        String name;
        List<VirtualReg> paramList = new ArrayList<>();
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

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        for (VirtualReg reg : vregOffsetMap.keySet()) {
            sb.append(String.format("%s: %d\n", reg.toString(), vregOffsetMap.get(reg)));
        }
        for (String funcName : funcMap.keySet()) {
            FunctionInfo funcInfo = funcMap.get(funcName);
            sb.append(String.format("func %s, size %d\n", funcName, funcInfo.size));
            for (VirtualReg param : funcInfo.paramList) {
                sb.append(String.format("  param %s: %s\n", param.name, vregOffsetMap.get(param).toString()));
            }
        }
        return sb.toString();
    }
}

package mips;

import intercode.Operand.VirtualReg;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

class FunctionInfo {
    String name;
    int frameSize;
    boolean hasCall;
    List<VirtualReg> paramList = new ArrayList<>();
    Set<Integer> regUseList = new HashSet<>();

    @Override
    public String toString() {
        return "FunctionInfo{" +
                "name='" + name + '\'' +
                ", frameSize=" + frameSize +
                ", hasCall=" + hasCall +
                ", paramList=" + paramList +
                ", regUseList=" + regUseList +
                '}';
    }
}

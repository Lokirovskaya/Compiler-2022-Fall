package mips;

import intercode.Operand.VirtualReg;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

class FunctionInfo {
    String name;
    int frameSize;
    boolean isPureFunc = true; // 没有调用其它函数
    List<VirtualReg> paramList = new ArrayList<>();
    Set<Integer> regUseSet = new HashSet<>();

    @Override
    public String toString() {
        return "FunctionInfo{" +
                "name='" + name + '\'' +
                ", frameSize=" + frameSize +
                ", isPureFunc=" + isPureFunc +
                ", paramList=" + paramList +
                ", regUseSet=" + regUseSet +
                '}';
    }
}

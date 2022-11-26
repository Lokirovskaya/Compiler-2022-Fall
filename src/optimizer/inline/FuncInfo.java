package optimizer.inline;

import intercode.Operand.VirtualReg;
import intercode.Quaternion;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

class FuncInfo {
    String name;
    List<VirtualReg> params;
    List<Quaternion> funcInter = new ArrayList<>();
    Set<VirtualReg> globalVarRef = new HashSet<>(); // 涉及到的全局变量
    boolean doNotInline = false; // 递归的函数、参数是数组的函数不能内联
}

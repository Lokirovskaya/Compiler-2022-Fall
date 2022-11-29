package optimizer.inline;

import intercode.Operand.InstNumber;
import intercode.Operand.VirtualReg;
import intercode.Quaternion;
import intercode.VirtualRegFactory;

import java.util.*;

import static intercode.Quaternion.OperatorType.SET;

// 将全局变量转化成局部变量（如果可能）
// 请在经过常数传播和折叠之后做
public class InlineGlobalVar {
    public static void run(List<Quaternion> inter) {
        List<Quaternion> globalInter = SplitFuncInter.getGlobalInter(inter);
        List<FuncInfo> funcInfoList = SplitFuncInter.splitFuncInter(inter);

        // 全局变量 => funcInfo 的字典
        // 如果此变量只在一个函数中出现，那么 value 是那个函数；若在多与一个函数中出现 value 是 null
        Map<VirtualReg, FuncInfo> gVarFuncMap = new HashMap<>();
        for (FuncInfo funcInfo : funcInfoList) {
            for (VirtualReg globalVar : funcInfo.globalVarRef) {
                if (gVarFuncMap.containsKey(globalVar))
                    gVarFuncMap.put(globalVar, null);
                else
                    gVarFuncMap.put(globalVar, funcInfo);
            }
        }
        gVarFuncMap.values().removeIf(v -> v == null);

        // 每个全局变量的值
        // 在常数折叠之后，这些值都应该是已知的
        Map<VirtualReg, InstNumber> gVarInstMap = new HashMap<>();
        for (Quaternion q : globalInter) {
            if (q.op == SET && q.target.isGlobal) {
                if (q.x1 instanceof InstNumber) {
                    gVarInstMap.put(q.target, (InstNumber) q.x1);
                }
            }
        }

        // 替换函数内所有对全局变量的引用为对 inlinedVar 的引用
        // 将需要内联的全局变量赋值为 inlinedVar，加入到每个函数的开头
        gVarFuncMap.forEach((var, func) -> {
            List<Quaternion> funcInter = func.funcInter;
            InstNumber inst = gVarInstMap.get(var);
            VirtualReg inlinedVar = VirtualRegFactory.newCopyReg(var);
            inlinedVar.isGlobal = false;

            for (Quaternion q : funcInter) {
                if (Objects.equals(q.target, var)) q.target = inlinedVar;
                if (Objects.equals(q.x1, var)) q.x1 = inlinedVar;
                if (Objects.equals(q.x2, var)) q.x2 = inlinedVar;
                for (int i = 0; q.list != null && i < q.list.size(); i++) {
                    if (Objects.equals(q.list.get(i), var)) q.list.set(i, inlinedVar);
                }
            }

            if (inst != null)
                funcInter.add(1, new Quaternion(SET, inlinedVar, inst, null, null));
            else
                funcInter.add(1, new Quaternion(SET, inlinedVar, var, null, null));
        });


        inter.clear();
        inter.addAll(globalInter);
        funcInfoList.forEach(f -> inter.addAll(f.funcInter));

        clearUnusedGlobalVar(inter);
    }

    private static void clearUnusedGlobalVar(List<Quaternion> inter) {

    }
}

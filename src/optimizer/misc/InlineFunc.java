package optimizer.misc;

import intercode.Label;
import intercode.Operand;
import intercode.Operand.VirtualReg;
import intercode.Quaternion;
import intercode.VirtualRegFactory;

import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

import static intercode.Quaternion.OperatorType.*;

public class InlineFunc {
    public static void run(List<Quaternion> inter) {
        List<Quaternion> globalInter = getGlobalInter(inter);
        List<FuncInfo> funcInfoList = splitFunc(inter);

        inline(funcInfoList);

        inter.clear();
        inter.addAll(globalInter);
        funcInfoList.forEach(f -> inter.addAll(f.funcInter));

        ClearLabel.run(inter);
    }

    private static class FuncInfo {
        String name;
        List<VirtualReg> params;
        List<Quaternion> funcInter = new ArrayList<>();
        boolean doNotInline = false; // 递归的函数、参数是数组的函数不能内联
    }

    private static void inline(List<FuncInfo> funcInfoList) {
        // 将 callee 内联进 caller
        for (FuncInfo callee : funcInfoList) {
            if (callee.name.equals("main") || callee.doNotInline) continue;

            for (FuncInfo caller : funcInfoList) {
                if (caller.name.equals(callee.name)) continue;

                for (int i = 0; i < caller.funcInter.size(); i++) {
                    Quaternion q = caller.funcInter.get(i);
                    if (q.op == CALL && q.label.name.equals(callee.name)) {
                        // inline here
                        // 获取内联处理后的 callee 部分的代码
                        assert q.list != null;
                        VirtualReg returnVreg = null;
                        if (i + 1 < caller.funcInter.size()) {
                            if (caller.funcInter.get(i + 1).op == GET_RETURN) {
                                returnVreg = caller.funcInter.get(i + 1).target;
                                caller.funcInter.remove(i + 1);
                            }
                        }
                        List<Quaternion> inlinedCalleeInter = getInlinedCalleeInter(callee, q.list, returnVreg);
                        // 用上述代码替换 caller 中的 call 语句
                        caller.funcInter.remove(i--);
                        caller.funcInter.addAll(i + 1, inlinedCalleeInter);
                    }
                }
            }
        }
        // 所有非递归函数最终都被内联到 main，因此只保留 main 和递归函数
        funcInfoList.removeIf(func -> !(func.name.equals("main") || func.doNotInline));
    }

    private static int inlineIdx = 0;

    // 获得 inline 之后的 callee inter
    // 参数：
    // callee：被调用的函数，此函数返回它的内联版本
    // callerParams：调用者提供的实参
    // returnVreg：调用者接收返回值的 vreg，可能为 null
    private static List<Quaternion> getInlinedCalleeInter(FuncInfo callee, List<Operand> callerParams, VirtualReg returnVreg) {
        // 初始化 result
        assert callee.funcInter.get(0).op == FUNC;
        inlineIdx++;
        List<Quaternion> result = new ArrayList<>();
        for (int i = 1; i < callee.funcInter.size(); i++) {
            Quaternion quaterOrigin = callee.funcInter.get(i);
            Quaternion quaterCopy = new Quaternion(quaterOrigin.op, quaterOrigin.target, quaterOrigin.x1, quaterOrigin.x2, quaterOrigin.label);
            if (quaterOrigin.list != null)
                quaterCopy.list = new ArrayList<>(quaterOrigin.list);
            result.add(quaterCopy);
        }

        // 替换四元式成分
        Map<VirtualReg, VirtualReg> renamedVregMap = new HashMap<>();
        Function<VirtualReg, VirtualReg> getRenamedVreg = vreg -> {
            assert !vreg.isGlobal;
            renamedVregMap.putIfAbsent(vreg, VirtualRegFactory.newCopyReg(vreg));
            return renamedVregMap.get(vreg);
        };
        Function<Operand, Boolean> shouldRename =
                o -> o instanceof VirtualReg && !((VirtualReg) o).isGlobal;
        Label endLabel = new Label("i" + inlineIdx + "end");

        for (int k = 0; k < result.size(); k++) {
            Quaternion q = result.get(k);
            // 重命名 vreg，排除全局变量 (isGlobal)
            if (shouldRename.apply(q.target)) q.target = getRenamedVreg.apply(q.target);
            if (shouldRename.apply(q.x1)) q.x1 = getRenamedVreg.apply((VirtualReg) q.x1);
            if (shouldRename.apply(q.x2)) q.x2 = getRenamedVreg.apply((VirtualReg) q.x2);
            for (int i = 0; q.list != null && i < q.list.size(); i++) {
                if (shouldRename.apply(q.list.get(i))) {
                    q.list.set(i, getRenamedVreg.apply((VirtualReg) q.list.get(i)));
                }
            }
            // 重命名跳转分支语句中所有的 label，加上后缀
            if (isJumpOrBranch(q) || q.op == LABEL) {
                q.label = new Label(q.label.name + "_i" + inlineIdx);
            }
            // 将所有的 return 替换为 goto inline_idx_end，最后一个 return 除外
            if (q.op == RETURN) {
                if (k != result.size() - 1)
                    result.set(k, new Quaternion(GOTO, null, null, null, endLabel));
                else
                    result.remove(k--);
            }
            // 将 set_return 换成对 returnVreg 的赋值
            if (q.op == SET_RETURN && returnVreg != null) {
                result.set(k, new Quaternion(SET, returnVreg, q.x1, null, null));
            }
        }

        // 添加 inline_idx_end 的 label
        result.add(new Quaternion(LABEL, null, null, null, endLabel));

        // 添加实参 -> 形参的赋值语句
        // 注意，callee.params 存储的是未重命名的形参
        assert callee.params.size() == callerParams.size();
        List<Quaternion> setParamsQuaters = new ArrayList<>(5);
        for (int i = 0; i < callee.params.size(); i++) {
            VirtualReg calleeP = callee.params.get(i);
            Operand callerP = callerParams.get(i);
            // 若不存在 renamed vreg，说明这个参数在 callee 中未被使用
            if (renamedVregMap.containsKey(calleeP)) {
                VirtualReg renamedCalleeP = getRenamedVreg.apply(calleeP);
                renamedCalleeP.isParam = false;
                setParamsQuaters.add(
                        new Quaternion(SET, renamedCalleeP, callerP, null, null)
                );
            }
        }
        result.addAll(0, setParamsQuaters);

        return result;
    }

    private static List<Quaternion> getGlobalInter(List<Quaternion> inter) {
        List<Quaternion> globalInter = new ArrayList<>();
        for (Quaternion q : inter) {
            if (q.op == FUNC) return globalInter;
            globalInter.add(q);
        }
        return globalInter;
    }

    private static List<FuncInfo> splitFunc(List<Quaternion> inter) {
        List<FuncInfo> result = new ArrayList<>();
        FuncInfo curFunc = null;
        for (Quaternion q : inter) {
            if (q.op == FUNC) {
                if (curFunc != null) result.add(curFunc);
                curFunc = new FuncInfo();
                curFunc.name = q.label.name;
                curFunc.params = q.list.stream().map(param -> (VirtualReg) param).collect(Collectors.toList());
                for (VirtualReg param : curFunc.params) {
                    if (param.isAddr) {
                        curFunc.doNotInline = true;
                        break;
                    }
                }
            }
            if (curFunc != null) {
                curFunc.funcInter.add(q);
                if (q.op == CALL && q.label.name.equals(curFunc.name))
                    curFunc.doNotInline = true;
            }
        }
        assert curFunc != null;
        result.add(curFunc);
        return result;
    }

    private static boolean isJumpOrBranch(Quaternion q) {
        return q.op == GOTO || q.op == IF || q.op == IF_NOT || q.op == IF_EQ || q.op == IF_NOT_EQ
                || q.op == IF_LESS || q.op == IF_GREATER || q.op == IF_LESS_EQ || q.op == IF_GREATER_EQ;
    }
}

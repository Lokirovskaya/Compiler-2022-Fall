package optimizer.inline;

import intercode.Operand;
import intercode.Quaternion;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import static intercode.Quaternion.OperatorType.CALL;
import static intercode.Quaternion.OperatorType.FUNC;

class SplitFuncInter {
    static List<Quaternion> getGlobalInter(List<Quaternion> inter) {
        List<Quaternion> globalInter = new ArrayList<>();
        for (Quaternion q : inter) {
            if (q.op == FUNC) return globalInter;
            globalInter.add(q);
        }
        return globalInter;
    }

    static List<FuncInfo> splitFuncInter(List<Quaternion> inter) {
        List<FuncInfo> result = new ArrayList<>();
        FuncInfo curFunc = null;
        for (Quaternion q : inter) {
            if (q.op == FUNC) {
                if (curFunc != null) result.add(curFunc);
                curFunc = new FuncInfo();
                curFunc.name = q.label.name;
                curFunc.params = q.list.stream().map(param -> (Operand.VirtualReg) param).collect(Collectors.toList());
                for (Operand.VirtualReg param : curFunc.params) {
                    if (param.isAddr) {
                        curFunc.doNotInline = true;
                        break;
                    }
                }
            }
            if (curFunc != null) {
                curFunc.funcInter.add(q);
                if (q.op == CALL && q.label.name.equals(curFunc.name)) {
                    curFunc.doNotInline = true;
                }
                curFunc.globalVarRef.addAll(q.getAllVregList()
                        .stream()
                        .filter(vreg -> vreg.isGlobal)
                        .collect(Collectors.toList()));
            }
        }
        assert curFunc != null;
        result.add(curFunc);
        return result;
    }
}

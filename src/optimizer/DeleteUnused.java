package optimizer;

import intercode.InterCode;
import intercode.Label;
import intercode.Operand.VirtualReg;

import java.util.HashSet;
import java.util.Set;

import static intercode.Quaternion.OperatorType.*;

class DeleteUnused {
    // 划分基本块：
    // func 是入口语句
    // if, if_not, goto, return 的下一条语句是入口语句
    // 删除无用 label 后，label 的下一条语句是起始语句
    // 基本块中不要包含 label，删除死代码之后再做一次清除 label 工作
    private static class BasicBlock{
        VirtualReg entrance;
        int len;
        BasicBlock[] next = new BasicBlock[2];
    }

    private static BasicBlock root;
//    private static Map<Label, >

    static void run(InterCode inter) {
        deleteUnusedLabel(inter);
        divideBasicBlock(inter);
    }


    private static void divideBasicBlock(InterCode inter) {
        inter.forEach(p-> {

        });
    }

    private static void deleteUnusedLabel(InterCode inter) {
        Set<Label> labelRef = new HashSet<>();
        inter.forEach(p -> {
            if (p.get().op == IF || p.get().op == IF_NOT || p.get().op == GOTO) {
                labelRef.add(p.get().label);
            }
        });
        inter.forEach(p -> {
            if (p.get().op == LABEL) {
                if (!labelRef.contains(p.get().label)) p.delete();
            }
        });
    }
}

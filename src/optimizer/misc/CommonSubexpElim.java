package optimizer.misc;

import intercode.Operand;
import intercode.Operand.VirtualReg;
import intercode.Quaternion;
import intercode.Quaternion.OperatorType;
import optimizer.block.Block;
import optimizer.block.FuncBlocks;
import optimizer.block.SplitBlock;
import util.Pair;
import util.UnorderedPair;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import static intercode.Quaternion.OperatorType.*;

// 消除基本块内公共子表达式
// 请在之前消除冗余赋值，否则效果会变差
public class CommonSubexpElim {
    public static void run(List<Quaternion> inter) {
        List<FuncBlocks> funcBlocksList = SplitBlock.split(inter);
        for (FuncBlocks funcBlocks : funcBlocksList) {
            for (Block block : funcBlocks.blockList) {
                runLVN(block.blockInter);
            }
        }

        SplitBlock.join(funcBlocksList, inter);
    }

    private static void runLVN(List<Quaternion> inter) {
        Map<Calc, VirtualReg> calcTargetMap = new HashMap<>();

        for (int i = 0; i < inter.size(); i++) {
            Quaternion q = inter.get(i);

            if (q.target != null && q.op != SET_ARRAY) {
                // 如果某个 vreg 被重新赋值，它对应的旧的 calc 无效
                calcTargetMap.values().removeIf(vreg -> vreg.equals(q.target));
                // 如果 calc 中某个 operand 被重新赋值，那么这个 calc 无效
                calcTargetMap.keySet().removeIf(calc -> calc.x.first.equals(q.target) || calc.x.second.equals(q.target));
            }

//            // 函数内联之后，同一块数组空间数组可能会有两个 vreg 指向，因此遇到 set_array 语句，移除所有数组相关项
//            if (q.op == SET_ARRAY) {
//                calcTargetMap.keySet().removeIf(calc -> calc.hasArray);
//            }

            // 遇到 call 时，移除可能变化的的 calc
            if (q.op == CALL) {
                calcTargetMap.keySet().removeIf(calc -> calc.hasGlobal || calc.hasArray);
            }

            if (isDualCalc(q.op)) {
                assert q.target != null && q.x1 != null && q.x2 != null;

                Calc calc;
                if (isCommunicative(q.op)) calc = new Calc(q.op, new UnorderedPair<>(q.x1, q.x2));
                else calc = new Calc(q.op, new Pair<>(q.x1, q.x2));

                if (calcTargetMap.containsKey(calc)) {
                    inter.set(i, new Quaternion(SET, q.target, calcTargetMap.get(calc), null, null));
                }
                else {
                    calcTargetMap.put(calc, q.target);
                }
            }
        }
    }

    // 一元运算少见且运算代价低，不优化也行
    private static boolean isDualCalc(OperatorType op) {
        return op == ADD || op == SUB || op == MULT || op == DIV || op == MOD ||
                op == EQ || op == NOT_EQ || op == LESS || op == LESS_EQ || op == GREATER || op == GREATER_EQ ||
                op == ADD_ADDR || op == GET_ARRAY;
    }

    private static boolean isCommunicative(OperatorType op) {
        return op == ADD || op == MULT || op == EQ || op == NOT_EQ || op == ADD_ADDR || op == GET_ARRAY;
    }
}


class Calc {
    OperatorType op;
    Pair<Operand, Operand> x;
    boolean hasGlobal, hasArray; // calc 含有全局变量或数组操作？

    public Calc(OperatorType op, Pair<Operand, Operand> x) {
        this.op = op;
        this.x = x;
        this.hasGlobal = (x.first instanceof VirtualReg && ((VirtualReg) x.first).isGlobal) ||
                (x.second instanceof VirtualReg && ((VirtualReg) x.second).isGlobal);
        this.hasArray = (x.first instanceof VirtualReg && ((VirtualReg) x.first).isAddr) ||
                (x.second instanceof VirtualReg && ((VirtualReg) x.second).isAddr);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Calc calc = (Calc) o;
        return op == calc.op && Objects.equals(x, calc.x);
    }

    @Override
    public int hashCode() {
        return Objects.hash(op, x);
    }
}
package optimizer.misc;

import intercode.Operand;
import intercode.Operand.VirtualReg;
import intercode.Quaternion;
import intercode.Quaternion.OperatorType;
import optimizer.block.Block;
import optimizer.block.FuncBlocks;
import optimizer.block.SplitBlock;

import java.util.*;
import java.util.stream.Collectors;

import static intercode.Quaternion.OperatorType.*;

public class LoopInvariantMotion {
    public static void run(List<Quaternion> inter) {
        List<FuncBlocks> funcBlocksList = SplitBlock.split(inter);
        for (FuncBlocks funcBlocks : funcBlocksList) {
            funcBlocks.doReachDefAnalysis();
            funcBlocks.printFuncBlocks();
            for (Block block : funcBlocks.blockList) {
                // 只优化最简单的循环（block 自环）
                if (block.jumpNext == block) {
                    runBlock(block);
                }
            }
        }
        SplitBlock.join(funcBlocksList, inter);
    }

    public static void runBlock(Block block) {
        // 循环不变量（不包括全局变量）
        Set<VirtualReg> invarVregSet;
        // 非循环不变量
        Set<VirtualReg> notInvarVregSet;

        // 若 vreg 在 in 中的所有定义：
        // 全部来自循环外：循环不变量
        // 部分来自循环里，部分来自循环外：不是循环不变量
        // 只来自循环里：不确定
        Set<VirtualReg> outBlock = new HashSet<>();
        Set<VirtualReg> inBlock = new HashSet<>();
        for (Quaternion q : block.reachDefFlow.in) {
            if (!isExp(q.op)) continue;
            assert q.target != null;
            if (q.block == block) inBlock.add(q.target);
            else outBlock.add(q.target);
        }
        Set<VirtualReg> tmp;
        // out - in
        tmp = new HashSet<>(outBlock);
        tmp.removeAll(inBlock);
        invarVregSet = new HashSet<>(tmp);
        // out ∩ in
        tmp = new HashSet<>(outBlock);
        tmp.retainAll(inBlock);
        notInvarVregSet = new HashSet<>(tmp);

        for (Quaternion q : block.blockInter) {
            // 若遇到了 ADD_ADDR 或 CALL，将涉及的地址 vreg 标记为非 invar
            if (q.op == ADD_ADDR || q.op == CALL) {
                notInvarVregSet.add(q.target);
                for (VirtualReg o : q.getUseVregList()) {
                    if (o.isAddr) {
                        notInvarVregSet.add(o);
                        invarVregSet.remove(o);
                    }
                }
                continue;
            }
            // 如果一个表达式所有的操作数都是 invar，那么表达式的 target 也是 invar
            if (!isExp(q.op)) continue;
            boolean isInvar = true;
            if (q.x1 instanceof VirtualReg) {
                if (!invarVregSet.contains(q.x1)) isInvar = false;
            }
            if (q.x2 instanceof VirtualReg) {
                if (!invarVregSet.contains(q.x2)) isInvar = false;
            }
            if (isInvar) {
                if (!notInvarVregSet.contains(q.target)) {
                    invarVregSet.add(q.target);
                }
            }
            else {
                invarVregSet.remove(q.target);
                notInvarVregSet.add(q.target);
            }
        }

        // 将 target 是 invar 的表达式计算语句移出循环（移到 block 开头）
        List<Quaternion> invarQuaterList = new ArrayList<>();
        block.blockInter.removeIf(q -> {
            if (isExp(q.op) && invarVregSet.contains(q.target)) {
                invarQuaterList.add(q);
                return true;
            }
            return false;
        });
        invarQuaterList.addAll(block.blockInter);
        block.blockInter = invarQuaterList;
    }

    private static boolean isExp(OperatorType op) {
        return op == SET || op == ADD || op == SUB || op == MULT || op == DIV || op == MOD || op == NEG ||
                op == NOT || op == EQ || op == NOT_EQ || op == LESS || op == LESS_EQ || op == GREATER || op == GREATER_EQ ||
                op == ADD_ADDR || op == GET_ARRAY || op == SET_ARRAY;
    }
}
package optimizer.block;

import intercode.InterCode;
import intercode.Quaternion;
import util.Wrap;

import java.util.ArrayList;
import java.util.List;

import static intercode.Quaternion.OperatorType.*;

public class SplitBlock {
    // 返回所有函数各自的基本块 root
    // label 是入口语句
    // goto, branch 的下一条语句是入口语句
    public static List<Block> run(InterCode inter) {
        List<Block> funcBlockRootList = new ArrayList<>();

        Wrap<Boolean> inFunc = new Wrap<>(false);
        Wrap<Block> curBlock = new Wrap<>(null);
        List<Block> allBlockList = new ArrayList<>();
        inter.forEach(p -> {
            // 跳过 .data 和全局数据，从第一个 func 开始
            if (p.get().op == FUNC && !inFunc.get()) {
                inFunc.set(true);
            }
            if (!inFunc.get()) return;

            // 新函数开始
            if (p.get().op == FUNC) {
                Block newBlock = new Block();
                curBlock.set(newBlock);
                funcBlockRootList.add(newBlock);
            }

            curBlock.get().blockInter.addLast(p.get());

            // 如果有 label，在表中记录 label 对应的 block
            if (p.get().op == LABEL) {
                Block.labelBlockMap.put(p.get().label, curBlock.get());
            }

            // 当前 label 结束。这里只记录 jumpNextLabel，jumpNext 在本次遍历结束后回填
            if (p.get().op == GOTO || isBranch(p.get().op) || (p.get(1) != null && p.get(1).op == LABEL)) {
                allBlockList.add(curBlock.get());
                if (isBranch(p.get().op)) {
                    curBlock.get().next = new Block();
                    curBlock.get().jumpNextLabel = p.get().label;
                    curBlock.set(curBlock.get().next);
                }
                else if (p.get().op == GOTO) {
                    curBlock.get().jumpNextLabel = p.get().label;
                    curBlock.set(new Block());
                }
                else {
                    curBlock.get().next = new Block();
                    curBlock.set(curBlock.get().next);
                }
            }
        });

        for (Block block : allBlockList) {
            if (block.jumpNextLabel != null) {
                block.jumpNext = Block.labelBlockMap.get(block.jumpNextLabel);
            }
        }
        return funcBlockRootList;
    }

    private static boolean isBranch(Quaternion.OperatorType op) {
        return op == IF || op == IF_NOT || op == IF_EQ || op == IF_NOT_EQ ||
                op == IF_LESS || op == IF_LESS_EQ || op == IF_GREATER || op == IF_GREATER_EQ;
    }
}

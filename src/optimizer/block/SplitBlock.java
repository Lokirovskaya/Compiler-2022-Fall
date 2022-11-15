package optimizer.block;

import intercode.Quaternion;
import util.Wrap;

import java.util.ArrayList;
import java.util.List;

import static intercode.Quaternion.OperatorType.*;

public class SplitBlock {
    // 返回所有函数各自的基本块 root
    // label 是入口语句
    // goto, branch 的下一条语句是入口语句
    public static List<FuncBlocks> split(List<Quaternion> inter) {
        List<FuncBlocks> funcBlocksList = new ArrayList<>();

        Wrap<Block> curBlock = new Wrap<>(null);
        Wrap<FuncBlocks> curFuncBlocks = new Wrap<>(null);
        // 全局区
        curFuncBlocks.set(new FuncBlocks());
        curFuncBlocks.get().funcName = ".global";
        Block globalBlock = new Block();
        curBlock.set(globalBlock);
        curFuncBlocks.get().root = globalBlock;

        for (int i = 0; i < inter.size(); i++) {
            Quaternion q = inter.get(i);// 新函数开始
            if (q.op == FUNC) {
                funcBlocksList.add(curFuncBlocks.get()); // 结算上一个
                curFuncBlocks.set(new FuncBlocks());
                curFuncBlocks.get().funcName = q.label.name;
                Block newBlock = new Block();
                curBlock.set(newBlock);
                curFuncBlocks.get().root = newBlock;
            }

            curBlock.get().blockInter.add(q);

            // 如果有 label，在表中记录 label 对应的 block
            if (q.op == LABEL) {
                Block.labelBlockMap.put(q.label, curBlock.get());
            }

            // 当前 Block 结束：
            // 1. 这条语句是 goto 或 branch（这里只记录 jumpNextLabel，jumpNext 在本次遍历结束后回填）
            // 2. 下一条语句是 label
            // 3. 下一条语句是 func，或没有下一条语句
            // 4. 这条语句是 return
            if (isBranch(q.op)) {
                curFuncBlocks.get().blockList.add(curBlock.get());
                curBlock.get().next = new Block();
                curBlock.get().jumpNextLabel = q.label;
                curBlock.set(curBlock.get().next);
            }
            else if (q.op == GOTO) {
                curFuncBlocks.get().blockList.add(curBlock.get());
                curBlock.get().jumpNextLabel = q.label;
                curBlock.set(new Block());
            }
            else if (i + 1 < inter.size() && inter.get(i + 1).op == LABEL) {
                curFuncBlocks.get().blockList.add(curBlock.get());
                curBlock.get().next = new Block();
                curBlock.set(curBlock.get().next);
            }
            else if (q.op == RETURN || i + 1 >= inter.size() || inter.get(i + 1).op == FUNC) {
                curFuncBlocks.get().blockList.add(curBlock.get());
                curBlock.set(new Block());
            }
        }
        // 结算 main
        if (curFuncBlocks.get() != null) {
            funcBlocksList.add(curFuncBlocks.get());
        }

        for (FuncBlocks funcBlocks : funcBlocksList) {
            for (Block block : funcBlocks.blockList) {
                if (block.jumpNextLabel != null) {
                    block.jumpNext = Block.labelBlockMap.get(block.jumpNextLabel);
                }
            }
        }
        return funcBlocksList;
    }

    private static boolean isBranch(Quaternion.OperatorType op) {
        return op == IF || op == IF_NOT || op == IF_EQ || op == IF_NOT_EQ ||
                op == IF_LESS || op == IF_LESS_EQ || op == IF_GREATER || op == IF_GREATER_EQ;
    }
}

package optimizer;

import intercode.InterCode;
import optimizer.block.Block;
import optimizer.block.FuncBlocks;
import optimizer.block.SplitBlock;

import java.util.List;

public class ClearDeadCode {
    static void run(InterCode inter) {
        List<FuncBlocks> funcBlocksList = SplitBlock.split(inter);
        funcBlocksList.forEach(func -> func.blockList.forEach(block -> block.isReachable = false));
        funcBlocksList.forEach(func -> runFuncBlocks(func.root));
        inter.clear();
        for (FuncBlocks funcBlocks : funcBlocksList) {
            for (Block block : funcBlocks.blockList) {
                if (block.isReachable) {
                    block.blockInter.forEachNode(p -> inter.addLast(p.get()));
                }
            }
        }
    }

    private static void runFuncBlocks(Block block) {
        block.isReachable = true;
        if (block.next != null && !block.next.isReachable) runFuncBlocks(block.next);
        if (block.jumpNext != null && !block.jumpNext.isReachable) runFuncBlocks(block.jumpNext);
    }
}

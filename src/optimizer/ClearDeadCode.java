package optimizer;

import intercode.Quaternion;
import optimizer.block.Block;
import optimizer.block.FuncBlocks;
import optimizer.block.SplitBlock;

import java.util.List;

public class ClearDeadCode {
    static void run(List<Quaternion> inter) {
        List<FuncBlocks> funcBlocksList = SplitBlock.split(inter);
        funcBlocksList.forEach(func -> func.blockList.forEach(block -> block.isReachable = false));
        funcBlocksList.forEach(func -> runFuncBlocks(func.root));
        inter.clear();
        for (FuncBlocks funcBlocks : funcBlocksList) {
            for (Block block : funcBlocks.blockList) {
                if (block.isReachable) {
                    inter.addAll(block.blockInter);
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

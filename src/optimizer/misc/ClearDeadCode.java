package optimizer.misc;

import intercode.Quaternion;
import optimizer.block.Block;
import optimizer.block.FuncBlocks;
import optimizer.block.SplitBlock;

import java.util.List;

public class ClearDeadCode {
    public static void run(List<Quaternion> inter) {
        List<FuncBlocks> funcBlocksList = SplitBlock.split(inter);
        funcBlocksList.forEach(func -> func.blockList.forEach(block -> block.visited = 0));
        funcBlocksList.forEach(func -> runFuncBlocks(func.root));
        inter.clear();
        for (FuncBlocks funcBlocks : funcBlocksList) {
            for (Block block : funcBlocks.blockList) {
                if (block.visited > 0) {
                    inter.addAll(block.blockInter);
                }
            }
        }
        ClearLabel.run(inter);
    }

    private static void runFuncBlocks(Block block) {
        block.visited = 1;
        if (block.next != null && block.next.visited == 0) runFuncBlocks(block.next);
        if (block.jumpNext != null && block.jumpNext.visited == 0) runFuncBlocks(block.jumpNext);
    }
}

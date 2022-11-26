package optimizer.constant;

import intercode.Quaternion;
import optimizer.block.Block;
import optimizer.block.FuncBlocks;
import optimizer.block.SplitBlock;

import java.util.*;

public class ConstPropagation {
    private static final Map<Block, LatticeSet> blockLatticeSetMap = new HashMap<>();

    public static void run(List<Quaternion> inter) {
        for (FuncBlocks funcBlocks : SplitBlock.split(inter)) {
            funcBlocks.blockList.forEach(block -> blockLatticeSetMap.put(block, new LatticeSet()));
            boolean changed;
            do {
                changed = runPass(funcBlocks.blockList);
            } while (changed);
        }
    }

    private static boolean runPass(List<Block> blockList) {
        boolean changed = false;
        for (Block block : blockList) {
            for (Block parent : block.parents) {
                if (blockLatticeSetMap.get(block).meetAll(blockLatticeSetMap.get(parent)))
                    changed = true;
            }
        }
        return changed;
    }

//    private static int calc(Quaternion.OperatorType opType, int a, int b) {
//
//    }
}

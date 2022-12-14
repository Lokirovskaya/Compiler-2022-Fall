package optimizer.block;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class FuncBlocks {
    public String funcName; // .global 表示全局区
    public Block root;
    public List<Block> blockList = new ArrayList<>();

    public void doLivenessAnalysis() {
        LivenessAnalysis.doAnalysis(this.blockList);
    }

    public void doReachDefAnalysis() {
        ReachDefAnalysis.doAnalysis(this.blockList);
    }

    public void printFuncBlocks() {
        System.out.println("func " + funcName);
        System.out.println("root " + root);
        for (Block block : blockList) {
            System.out.printf("this %x, next %x, jumpNext %x, parents: %s\n",
                    block.hashCode(), block.next == null ? null : block.next.hashCode(), block.jumpNext == null ? null : block.jumpNext.hashCode(), block.parents);
            if (block.livenessFlow != null) {
                System.out.printf("in: %s, out: %s, def: %s, use: %s\n", Arrays.toString(block.livenessFlow.in.toArray()), Arrays.toString(block.livenessFlow.out.toArray()), Arrays.toString(block.livenessFlow.def.toArray()), Arrays.toString(block.livenessFlow.use.toArray()));
            }
            if (block.reachDefFlow != null) {
                System.out.printf("in: %s, out: %s, gen: %s, kill: %s\n", Arrays.toString(block.reachDefFlow.in.toArray()), Arrays.toString(block.reachDefFlow.out.toArray()), Arrays.toString(block.reachDefFlow.gen.toArray()), Arrays.toString(block.reachDefFlow.kill.toArray()));
            }
            block.blockInter.forEach(q -> System.out.println("  " + q.id + " " + q));
        }
        System.out.println();
    }
}

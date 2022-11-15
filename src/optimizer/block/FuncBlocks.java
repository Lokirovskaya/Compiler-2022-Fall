package optimizer.block;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class FuncBlocks {
    public String funcName; // .global 表示全局区
    public Block root;
    public List<Block> blockList = new ArrayList<>();

    public void printFuncBlocks() {
        System.out.println("func " + funcName);
        System.out.println("root " + root);
        for (Block block : blockList) {
            System.out.printf("this %x, next %x, jumpNext %x\n",
                    block.hashCode(), block.next == null ? null : block.next.hashCode(), block.jumpNext == null ? null : block.jumpNext.hashCode());
            if (block.in != null) {
                System.out.printf("in: %s, out: %s, def: %s, use: %s\n", Arrays.toString(block.in.toArray()), Arrays.toString(block.out.toArray()), Arrays.toString(block.def.toArray()), Arrays.toString(block.use.toArray()));
            }
            block.blockInter.forEach(q -> System.out.println("  " + q.id + " " + q));
        }
        System.out.println();
    }
}

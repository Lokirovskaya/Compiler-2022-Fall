package optimizer.block;

import java.util.ArrayList;
import java.util.List;

public class FuncBlocks {
    public String funcName; // .global 表示全局区
    public Block root;
    public List<Block> blockList = new ArrayList<>();

    public void printFuncBlocks() {
        System.out.println("func " + funcName);
        System.out.println("root " + root);
        for (Block block : blockList) {
            System.out.printf("this %x, next %x, jumpNext %x %s\n",
                    block.hashCode(), block.next == null ? null : block.next.hashCode(), block.jumpNext == null ? null : block.jumpNext.hashCode(),
                    block.isReachable ? "" : "(Dead)");
            block.blockInter.forEach(p -> System.out.println("  " + p.get().toString()));
        }
        System.out.println();
    }
}

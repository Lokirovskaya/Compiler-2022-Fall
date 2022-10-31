package optimizer.block;

import intercode.InterCode;
import intercode.Label;

import java.util.HashMap;
import java.util.Map;

public class Block {
    public Block next, jumpNext;
    public InterCode blockInter = new InterCode();
    public Label jumpNextLabel;

    public static Map<Label, Block> labelBlockMap = new HashMap<>();

    public void printBlock() {
        System.out.printf("Block: %s, next: %s, jumpNext: %s\n", this, next, jumpNextLabel);
        blockInter.forEach(p-> System.out.println(p.get().toString()));
        System.out.print("\n");
    }
}

package optimizer.block;

import intercode.InterCode;
import intercode.Label;

import java.util.HashMap;
import java.util.Map;

public class Block {
    public Block next, jumpNext;
    public InterCode blockInter = new InterCode();
    public Label jumpNextLabel;
    public boolean isReachable;

    public static Map<Label, Block> labelBlockMap = new HashMap<>();
}

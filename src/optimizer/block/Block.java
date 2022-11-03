package optimizer.block;

import intercode.InterCode;
import intercode.Label;
import intercode.Operand;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

public class Block {
    public Block next, jumpNext;
    public InterCode blockInter = new InterCode();
    public Label jumpNextLabel;
    // 可达性分析
    public boolean isReachable;
    // 活跃变量分析
    public Set<Operand.VirtualReg> in, out, use, def;

    public static Map<Label, Block> labelBlockMap = new HashMap<>();
}

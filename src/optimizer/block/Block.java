package optimizer.block;

import intercode.Label;
import intercode.Operand;
import intercode.Quaternion;

import java.util.*;

public class Block {
    public Block next, jumpNext;
    public List<Block> parents = new ArrayList<>(2);
    public List<Quaternion> blockInter = new ArrayList<>();
    public Label jumpNextLabel;
    // 可达性分析
    public boolean isReachable;
    // 活跃变量分析
    public Set<Operand.VirtualReg> in, out, use, def;

    public static Map<Label, Block> labelBlockMap = new HashMap<>();
}

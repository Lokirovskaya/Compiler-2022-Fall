package optimizer.block;

import intercode.Label;
import intercode.Operand;
import intercode.Operand.VirtualReg;
import intercode.Quaternion;

import java.util.*;

public class Block {
    public Block next, jumpNext;
    public List<Block> parents = new ArrayList<>(2);
    public List<Quaternion> blockInter = new ArrayList<>();
    public Label jumpNextLabel;
    public static Map<Label, Block> labelBlockMap = new HashMap<>();
    // 可达性分析
    public boolean isReachable;


    // 活跃变量分析
    public LivenessFlow livenessFlow;
    // 可用表达式分析
    public ReachDefAnalysis reachDefAnalysis;

    public static class LivenessFlow {
        public Set<VirtualReg>
                in = new HashSet<>(),
                out = new HashSet<>(),
                use = new HashSet<>(),
                def = new HashSet<>();
    }

    public static class ReachDefAnalysis {
        public Set<VirtualReg>
                in = new HashSet<>(),
                out = new HashSet<>(),
                gen = new HashSet<>(),
                kill = new HashSet<>();
    }
}

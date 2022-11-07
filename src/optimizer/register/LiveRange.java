package optimizer.register;

import intercode.Operand;

import java.util.ArrayList;
import java.util.List;

public class LiveRange {
    public Operand.VirtualReg vreg;
    public int start, end; // 左右闭区间
    public int realReg = -1;
    public List<Integer> usePointList = new ArrayList<>();

    public LiveRange(Operand.VirtualReg vreg, int start, int end) {
        this.vreg = vreg;
        this.start = start;
        this.end = end;
    }

    @Override
    public String toString() {
        return String.format("{vreg: %s, reg: %d, range: [%d, %d], use: %s}", vreg.toString(), realReg, start, end, usePointList);
    }
}

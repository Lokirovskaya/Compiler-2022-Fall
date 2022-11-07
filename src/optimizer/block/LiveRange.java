package optimizer.block;

import intercode.Operand;

public class LiveRange {
    public Operand.VirtualReg vreg;
    public int start, end; // 左右闭区间
    public boolean freed = false;

    public LiveRange(Operand.VirtualReg vreg, int start, int end) {
        this.vreg = vreg;
        this.start = start;
        this.end = end;
    }

    @Override
    public String toString() {
        return String.format("{%s (%d, %d)}", vreg.toString(), start, end);
    }
}

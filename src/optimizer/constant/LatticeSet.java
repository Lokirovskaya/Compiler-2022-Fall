package optimizer.constant;

import intercode.Operand.VirtualReg;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

public class LatticeSet {
    final Map<VirtualReg, Lattice> vregLatticeMap = new HashMap<>();

    // 相同的集合才能 meetAll
    // 返回：集合是否发生改变
    boolean meetAll(LatticeSet that) {
        assert this.vregLatticeMap.size() == that.vregLatticeMap.size();
        boolean changed = false;
        for (VirtualReg vreg : vregLatticeMap.keySet()) {
            assert that.vregLatticeMap.containsKey(vreg);
            Lattice thisLattice = this.vregLatticeMap.get(vreg);
            Lattice thatLattice = that.vregLatticeMap.get(vreg);
            Lattice meetResult = thisLattice.meet(thatLattice);
            if (thisLattice != meetResult) {
                changed = true;
            }
            this.vregLatticeMap.put(vreg, meetResult);
        }
        return changed;
    }

    void setConst(VirtualReg vreg, int number) {
        this.vregLatticeMap.get(vreg).type = Lattice.LatticeType.CONST;
        this.vregLatticeMap.get(vreg).number = number;
    }
}

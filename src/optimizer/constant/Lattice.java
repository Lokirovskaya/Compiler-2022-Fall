package optimizer.constant;

import intercode.Operand.VirtualReg;

import static optimizer.constant.Lattice.LatticeType.*;

public class Lattice {
    LatticeType type;
    int number;
    VirtualReg virtualReg;

    enum LatticeType {
        UNDEF, CONST, NAC
    }

    public Lattice(LatticeType type, VirtualReg virtualReg) {
        this.virtualReg = virtualReg;
        this.type = type;
    }

    Lattice meet(Lattice that) {
        assert this.virtualReg == that.virtualReg;
        if (this.type == UNDEF && that.type == UNDEF)
            return this;
        else if (this.type == UNDEF && that.type == CONST)
            return this;
        else if (this.type == UNDEF && that.type == NAC)
            return that;
        else if (this.type == CONST && that.type == UNDEF)
            return that;
        else if (this.type == CONST && that.type == CONST) {
            if (this.number == that.number) return this;
            else return new Lattice(NAC, this.virtualReg);
        }
        else if (this.type == CONST && that.type == NAC)
            return that;
        else if (this.type == NAC && that.type == UNDEF)
            return this;
        else if (this.type == NAC && that.type == CONST)
            return this;
        else if (this.type == NAC && that.type == NAC)
            return this;
        return null;
    }
}

package intercode;

import symbol.Symbol;

public abstract class Operand {
    public static class VirtualReg extends Operand {
        public int regID, realReg;
        public Symbol.Var var; // null 表示临时变量

        public VirtualReg(int r) {
            this.regID = r;
        }
    }

    public static class InstNumber extends Operand {
        public int number;

        public InstNumber(int i) {
            this.number = i;
        }
    }

    public static class Label extends Operand {
        public String name;

        public Label(String s) {
            this.name = s;
        }
    }

    @Override
    public String toString() {
        if (this instanceof VirtualReg) {
            VirtualReg reg = (VirtualReg) this;
            if (reg.var != null)
                return String.format("@%d:%s_%d", reg.regID, reg.var.name, reg.var.selfTable.id);
            else return "@" + reg.regID;
        }
        else if (this instanceof InstNumber) return String.valueOf(((InstNumber) this).number);
        else return ((Label) this).name;
    }
}

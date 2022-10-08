package intercode;

public abstract class Operand {
    public static class VirtualReg extends Operand {
        public int reg, realReg;

        public VirtualReg(int reg) {
            this.reg = reg;
        }
    }

    public static class InstNumber extends Operand {
        public int number;

        public InstNumber(int number) {
            this.number = number;
        }
    }

    public static class Label extends Operand {
        public String name;

        public Label(String name) {
            this.name = name;
        }
    }

    @Override
    public String toString() {
        if (this instanceof VirtualReg) return "@" + ((VirtualReg) this).reg;
        else if (this instanceof InstNumber) return String.valueOf(((InstNumber) this).number);
        else return ((Label) this).name;
    }
}

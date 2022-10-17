package intercode;

public abstract class Operand {
    public static class VirtualReg extends Operand {
        public int regID, realReg;
        public boolean declareConst = false;
        public boolean isAddr = false;
        public String name;

        public VirtualReg(int r) {
            this.regID = r;
        }

        public VirtualReg(int r, String name) {
            this.regID = r;
            this.name = name;
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
            if (reg.isAddr) return "%&" + ((reg.name == null) ? reg.regID : reg.name);
            else return "%" + ((reg.name == null) ? reg.regID : reg.name);
        }
        else if (this instanceof InstNumber) return String.valueOf(((InstNumber) this).number);
        else return ((Label) this).name;
    }
}


package intercode;

public abstract class Operand {
    public static class VirtualReg extends Operand {
        public int regID;
        public int realReg = -1; // 分配了的实寄存器
        public int tableID;
        public boolean isAddr;
        public boolean isGlobal;
        public String name;

        public VirtualReg(int r) {
            this.regID = r;
        }

        @Override
        public String toString() {
            StringBuilder sb = new StringBuilder();
            sb.append('@');
            if (isGlobal) sb.append('@');
            if (isAddr) sb.append('&');
            sb.append(regID);
            if (name != null) {
                sb.append('_').append(name);
            }
            if (realReg >= 0) sb.append('$').append(realReg);
            return sb.toString();
        }
    }

    public static class InstNumber extends Operand {
        public int number;

        public InstNumber(int i) {
            this.number = i;
        }

        @Override
        public String toString() {
            return String.valueOf(number);
        }
    }
}


package intercode;

import java.util.Objects;

public abstract class Operand {
    public static class VirtualReg extends Operand {
        public int regID;
        public boolean isAddr;
        public boolean isParam; // 形参？
        public boolean isGlobal;
        // 储存管理相关
        public int realReg = -1;
        public int stackOffset = -1; // -1 表示未在栈上分配

        public VirtualReg(int regID) {
            this.regID = regID;
        }

        @Override
        public String toString() {
            StringBuilder sb = new StringBuilder();
            sb.append('@');
            if (isGlobal) sb.append('@');
            if (isAddr) sb.append('&');
            sb.append(regID);
            return sb.toString();
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            VirtualReg that = (VirtualReg) o;
            return regID == that.regID;
        }

        @Override
        public int hashCode() {
            return Objects.hash(regID);
        }
    }

    public static class InstNumber extends Operand {
        public int number;
        // 值是否 = 2147483648，如果是，设为 true，在与负号计算得到 -2147483648 之后填入 number
        // 只在生成中间代码时启用
        boolean overflow = false;

        public InstNumber(int i) {
            this.number = i;
        }

        @Override
        public String toString() {
            return String.valueOf(number);
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            InstNumber that = (InstNumber) o;
            return number == that.number && overflow == that.overflow;
        }

        @Override
        public int hashCode() {
            return Objects.hash(number, overflow);
        }
    }
}


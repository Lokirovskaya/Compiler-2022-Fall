package intercode;

import optimizer.register.LiveRange;

import java.util.List;

public abstract class Operand {
    public static class VirtualReg extends Operand {
        public int regID;
        public int tableID;
        public boolean isAddr;
        public boolean isParam;
        public boolean isGlobal;
        public String name;
        // 储存管理相关
        public List<LiveRange> regRangeList;
        public int stackOffset = -1; // -1 表示未在栈上分配

        public VirtualReg(int regID) {
            this.regID = regID;
        }

        // 获取 this 在某一行的寄存器，未分配返回 -1
        public int getRealReg(int lineNumber) {
            if (lineNumber <= 0) return -1;
            if (regRangeList == null) return -1;
            for (LiveRange range : regRangeList) {
                if (range.start <= lineNumber && lineNumber <= range.end) {
                    return range.realReg;
                }
            }
            return -1;
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


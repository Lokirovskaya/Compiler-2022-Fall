package mips;

import util.NodeList;

class MipsUtil {
    private static final String[] regNames = {
            "zero", "at", "v0", "v1",
            "a0", "a1", "a2", "a3",
            "t0", "t1", "t2", "t3", "t4", "t5", "t6", "t7",
            "s0", "s1", "s2", "s3", "s4", "s5", "s6", "s7",
            "t8", "t9", "k0", "k1",
            "gp", "sp", "fp", "ra"
    };

    static String getRegName(int reg) {
        return "$" + regNames[reg];
    }

    static void optimize(NodeList<String> mips) {
        mips.forEach(p -> {
            // 优化连续相同的 lw+sw 操作
            // lw+sw->lw; sw+lw->sw
            if (p.get(1) != null) {
                if (p.get().startsWith("lw") && p.get(1).startsWith("sw") || p.get().startsWith("sw") && p.get(1).startsWith("lw")) {
                    if (p.get().substring(2).equals(p.get(1).substring(2))) {
                        p.delete(1);
                    }
                }
            }
            // 删除无用的 move $xx, $xx
            //          0123456789012
            if (p.get().startsWith("move")) {
                if (p.get().substring(5, 8).equals(p.get().substring(10, 13))) {
                    p.delete();
                }
            }
        });
    }
}

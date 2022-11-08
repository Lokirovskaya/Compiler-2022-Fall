package mips;

import util.NodeList;
import util.Wrap;

class MipsOptimizer {
    static void optimize(NodeList<Mips> mipsList) {
        peepHole(mipsList);
    }

    private static void peepHole(NodeList<Mips> mips) {
        Wrap<Boolean> inText = new Wrap<>(false);
        mips.forEachNode(p -> {
            // 跳过 .data
            if (p.get().code.equals(".text")) {
                inText.set(true);
                return;
            }
            if (!inText.get()) return;

            String[] args = p.get().args;
            // 优化 lw+sw 操作
            // lw+sw->lw; sw+lw->sw
            // sw $t1,a + lw $t2,a -> sw $t1,a + move $t2,$t1
            if (p.get(1) != null) {
                if (args[0].equals("lw") && p.get(1).args[0].equals("sw") || args[0].equals("sw") && p.get(1).args[0].equals("lw")) {
                    if (args[1].equals(p.get(1).args[1]) && args[2].equals(p.get(1).args[2]) && args[3].equals(p.get(1).args[3])) {
                        p.delete(1);
                    }
                }
                if (args[0].equals("sw") && p.get(1).args[0].equals("lw")) {
                    if (args[2].equals(p.get(1).args[2]) && args[3].equals(p.get(1).args[3])) {
                        p.set(1, new Mips(String.format("move %s, %s", p.get(1).args[1], args[1])));
                    }
                }
            }
            // 删除无用的指令
            // jr $ra + jr $ra
            if (p.get(1) != null) {
                if (p.get().code.equals("jr $ra") && p.get().code.equals(p.get(1).code)) {
                    p.delete();
                }
            }
            // move $t, $t
            if (args[0].equals("move")) {
                if (args[1].equals(args[2])) {
                    p.delete();
                }
            }
            // add|sub $t, $t, 0
            if (args[0].equals("add") || args[0].equals("sub")) {
                if (args[1].equals(args[2]) && isZero(args[3])) {
                    p.delete();
                }
            }
        });
    }

    private static boolean isZero(String str) {
        return str.equals("0") || str.equals("-0") || str.equals("$0") || str.equals("$zero");
    }
}

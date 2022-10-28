package mips;

import util.NodeList;
import util.Wrap;

class MipsOptimizer {
    static void optimize(NodeList<Mips> mipsList) {
        peepHole(mipsList);
    }

    private static void peepHole(NodeList<Mips> mips) {
        Wrap<Boolean> inText = new Wrap<>(false);
        mips.forEach(p -> {
            if (p.get().code.equals(".text")) {
                inText.set(true);
                return;
            }
            if (!inText.get()) return;
            // 优化 lw+sw 操作
            // lw+sw->lw; sw+lw->sw
            // sw $t1,a + lw $t2,a -> sw $t1,a + move $t2,$t1
            if (p.get(1) != null) {
                if (p.get().op.equals("lw") && p.get(1).op.equals("sw") || p.get().op.equals("sw") && p.get(1).op.equals("lw")) {
                    if (p.get().x1.equals(p.get(1).x1) && p.get().x2.equals(p.get(1).x2) && p.get().x3.equals(p.get(1).x3)) {
                        p.delete(1);
                    }
                }
                if (p.get().op.equals("sw") && p.get(1).op.equals("lw")) {
                    if (p.get().x2.equals(p.get(1).x2) && p.get().x3.equals(p.get(1).x3)) {
                        p.set(new Mips(String.format("move %s, %s", p.get(1).x1, p.get(0).x1)), 1);
                    }
                }
            }
            // 删除无用的 move $t, $t
            if (p.get().op.equals("move")) {
                if (p.get().x1.equals(p.get().x2)) {
                    p.delete();
                }
            }
        });
    }
}

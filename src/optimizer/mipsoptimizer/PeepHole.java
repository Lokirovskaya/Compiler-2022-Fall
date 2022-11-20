package optimizer.mipsoptimizer;

import mips.Mips;

import java.util.List;

public class PeepHole {
    public static void run(List<Mips> mipsList) {
        boolean inTextSegment = false;
        for (int i = 0; i < mipsList.size(); i++) {
            if (mipsList.get(i).code.equals(".text")) {
                inTextSegment = true;
                continue;
            }
            if (!inTextSegment) continue;

            String[] args = mipsList.get(i).args;
            // 优化 lw+sw 操作
            // lw+sw->lw; sw+lw->sw
            // sw $t1,a + lw $t2,a -> sw $t1,a + move $t2,$t1
            if (i + 1 < mipsList.size()) {
                if (args[0].equals("lw") && mipsList.get(i + 1).args[0].equals("sw") || args[0].equals("sw") && mipsList.get(i + 1).args[0].equals("lw")) {
                    if (args[1].equals(mipsList.get(i + 1).args[1]) && args[2].equals(mipsList.get(i + 1).args[2]) && args[3].equals(mipsList.get(i + 1).args[3])) {
                        mipsList.remove(i + 1);
                    }
                }
                if (args[0].equals("sw") && mipsList.get(i + 1).args[0].equals("lw")) {
                    if (args[2].equals(mipsList.get(i + 1).args[2]) && args[3].equals(mipsList.get(i + 1).args[3])) {
                        mipsList.set(i + 1, new Mips(String.format("move %s, %s", mipsList.get(i + 1).args[1], args[1])));
                    }
                }
            }
            // 删除无用的指令
            // jr $ra + jr $ra
            if (i + 1 < mipsList.size()) {
                if (mipsList.get(i).code.equals("jr $ra") && mipsList.get(i).code.equals(mipsList.get(i + 1).code)) {
                    mipsList.remove(i--);
                }
            }
            // move $t,$t
            // move $t1,$t2; move $t2,$t1 -> move $t1,$t2
            if (args[0].equals("move")) {
                if (args[1].equals(args[2])) {
                    mipsList.remove(i--);
                }
                if (i + 1 < mipsList.size()) {
                    if (mipsList.get(i + 1).args[0].equals("move")) {
                        if (args[1].equals(mipsList.get(i + 1).args[2]) && args[2].equals(mipsList.get(i + 1).args[1])) {
                            mipsList.remove(i + 1);
                        }
                    }
                }
            }
            // add|sub|sra|srl|sll $t,$t,0
            if (args[0].equals("add") || args[0].equals("sub") ||
                    args[0].equals("sra") || args[0].equals("srl") || args[0].equals("sll")) {
                if (args[1].equals(args[2]) && isZero(args[3])) {
                    mipsList.remove(i--);
                }
            }

        }
    }

    private static boolean isZero(String str) {
        return str.equals("0") || str.equals("-0") || str.equals("$0") || str.equals("$zero");
    }
}

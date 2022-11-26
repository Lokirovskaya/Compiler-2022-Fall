package optimizer.mipsoptimizer;

import mips.Mips;
import util.Pair;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import static java.lang.Math.abs;

public class WeakenDiv {
    public static void run(List<Mips> mipsList) {
        // div $targetReg, $divReg, inst
        for (int i = 0; i < mipsList.size(); i++) {
            String[] args = mipsList.get(i).args;
            if (!(args[0].equals("div") && isNumber(args[3]))) continue;

            // 为避免数据冲突，请注意：
            // 1. 写 targetReg 之后，不能再读 divReg
            // 2. 不允许写 divReg
            // 3. 临时寄存器使用 $t9, 不能使用 $t8
            String targetReg = args[1];
            String divReg = args[2];
            int inst = Integer.parseInt(args[3]);
            List<String> codeList = new ArrayList<>();
            assert inst != 0;

            int l = ceilLog2(abs(inst));
            if (inst == 1) {
                codeList.add(String.format("move %s, %s", targetReg, divReg));
            }
            else if (inst == -1) {
                codeList.add(String.format("neg %s, %s", targetReg, divReg));
            }
            else if (inst == -2147483648) {
                // n == -2147483648 ? 1 : 0;
                codeList.add(String.format("seq %s, %s, -2147483648", targetReg, divReg));
            }
            else if (abs(inst) == (1L << l)) {
                // ans = sra(n + srl(sra(n, l - 1), 32 - l), l);
                codeList.add(String.format("sra $t9, %s, %d", divReg, l - 1));
                codeList.add(String.format("srl $t9, $t9, %d", 32 - l));
                codeList.add(String.format("add $t9, %s, $t9", divReg));
                codeList.add(String.format("sra %s, $t9, %d", targetReg, l));
                if (inst < 0) codeList.add(String.format("neg %s, %s", targetReg, targetReg));
            }
            else {
                Pair<Long, Integer> ms = chooseMultiplier(abs(inst), 31);
                long m = ms.first;
                int s = ms.second;
                if (m < (1L << 31)) {
                    // ans = sra(mulsh(m, n), s) - xsign(n);
                    codeList.add(String.format("mul $t9, %s, %d", divReg, m));
                    codeList.add("mfhi $t9");
                }
                else {
                    // ans = sra(n + mulsh(m - (1L << 32), n), s) - xsign(n);
                    codeList.add(String.format("mul $t9, %s, %d", divReg, m - (1L << 32)));
                    codeList.add("mfhi $t9");
                    codeList.add(String.format("add $t9, $t9, %s", divReg));
                }
                codeList.add(String.format("sra $t9, $t9, %d", s));
                codeList.add(String.format("sra %s, %s, 31", targetReg, divReg));
                codeList.add(String.format("sub %s, $t9, %s", targetReg, targetReg));
                if (inst < 0) codeList.add(String.format("neg %s, %s", targetReg, targetReg));
            }

            mipsList.remove(i--);
            List<Mips> mulMipsList = codeList.stream()
                    .map(str -> new Mips(str))
                    .collect(Collectors.toList());
            mipsList.addAll(i + 1, mulMipsList);
        }
    }

    // return (m, s)
    static Pair<Long, Integer> chooseMultiplier(int inst, int bit) {
        assert inst > 0;
        int s = ceilLog2(inst);
        long low = Long.divideUnsigned(1L << (32 + s), inst);
        long high = Long.divideUnsigned((1L << (32 + s)) + (1L << (32 + s - bit)), inst);
        while (Long.compareUnsigned(low >>> 1, high >>> 1) < 0 && s > 0) {
            low >>>= 1;
            high >>>= 1;
            s--;
        }
        return new Pair<>(high, s);
    }

    private static int ceilLog2(int x) {
        int s = 0;
        while ((1L << s) < x) s++;
        return s;
    }

    private static boolean isNumber(String str) {
        return str != null && str.matches("^[+-]?[0-9]+$");
    }
}
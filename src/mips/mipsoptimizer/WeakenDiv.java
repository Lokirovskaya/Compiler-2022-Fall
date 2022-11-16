package mips.mipsoptimizer;

import mips.Mips;
import util.Pair;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import static java.lang.Math.abs;

public class WeakenDiv {
    public static void run(List<Mips> mipsList) {
        // li $t9, instNumber
        // div $divReg, $t9
        // mflo $targetReg
        for (int i = 0; i < mipsList.size() - 2; i++) {
            boolean found = false;
            if (mipsList.get(i).args[0].equals("li") && mipsList.get(i).args[1].equals("$t9")) {
                if (mipsList.get(i + 1).args[0].equals("div") && mipsList.get(i + 1).args[2].equals("$t9")) {
                    if (mipsList.get(i + 2).args[0].equals("mflo")) {
                        found = true;
                    }
                }
            }
            if (!found) continue;

            String targetReg = mipsList.get(i + 2).args[1];
            String divReg = mipsList.get(i + 1).args[1];
            int div = Integer.parseInt(mipsList.get(i).args[2]);
            List<String> codeList = new ArrayList<>();
            assert div != 0;

            int l = ceilLog2(abs(div));
            if (div == 1) {
                codeList.add(String.format("move %s, %s", targetReg, div));
            }
            else if (div == -1) {
                codeList.add(String.format("neg %s, %s", targetReg, div));
            }
            else if (div == -2147483648) {
                // n == -2147483648 ? 1 : 0;
                codeList.add(String.format("seq %s, %s, -2147483648", targetReg, div));
            }
            else if (abs(div) == (1L << l)) {
                // ans = sra(n + srl(sra(n, l - 1), 32 - l), l);
                codeList.add(String.format("sra $t9, %s, %d", divReg, l - 1));
                codeList.add(String.format("srl $t9, $t9, %d", 32 - l));
                codeList.add(String.format("add $t9, %s, $t9", divReg));
                codeList.add(String.format("sra %s, $t9, %d", targetReg, l));
                if (div < 0) codeList.add(String.format("neg %s, %s", targetReg, targetReg));
            }
            else {
                Pair<Long, Integer> ms = chooseMultiplier(abs(div), 31);
                long m = ms.first;
                int s = ms.second;
                if (m < (1L << 31)) {
                    // ans = sra(mulsh(m, n), s) - xsign(n);
                    codeList.add(String.format("mul $t9, %s, %d", divReg, m));
                    codeList.add("mfhi $t9");
                    codeList.add(String.format("sra $t8, $t9, %d", s));
                    codeList.add(String.format("sra $t9, %s, 31", divReg));
                    codeList.add(String.format("sub %s, $t8, $t9", targetReg));
                    if (div < 0) codeList.add(String.format("neg %s, %s", targetReg, targetReg));
                }
                else {
                    // ans = sra(n + mulsh(m - (1L << 32), n), s) - xsign(n);
                    codeList.add(String.format("mul $t9, %s, %d", divReg, m - (1L << 32)));
                    codeList.add("mfhi $t9");
                    codeList.add(String.format("add $t9, $t9, %s", divReg));
                    codeList.add(String.format("sra $t8, $t9, %d", s));
                    codeList.add(String.format("sra $t9, %s, 31", divReg));
                    codeList.add(String.format("sub %s, $t8, $t9", targetReg));
                    if (div < 0) codeList.add(String.format("neg %s, %s", targetReg, targetReg));
                }
            }

            mipsList.remove(i + 2);
            mipsList.remove(i + 1);
            mipsList.remove(i--);
            List<Mips> mulMipsList = codeList.stream()
                    .map(str -> new Mips(str))
                    .collect(Collectors.toList());
            mipsList.addAll(i + 1, mulMipsList);
        }
    }

    // return (m, s)
    static Pair<Long, Integer> chooseMultiplier(int div, int bit) {
        assert div > 0;
        int s = ceilLog2(div);
        long low = Long.divideUnsigned(1L << (32 + s), div);
        long high = Long.divideUnsigned((1L << (32 + s)) + (1L << (32 + s - bit)), div);
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
}
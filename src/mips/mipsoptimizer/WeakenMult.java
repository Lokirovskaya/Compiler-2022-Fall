package mips.mipsoptimizer;

import mips.Mips;
import util.Pair;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

// 乘法优化，由于 mul 指令只需要 4 ticks，因此只有以下三种情况优化有效：
// 对于 a = b * m (m > 0)
// 1. m = 2^k，化为 a = b << k
// 2. m = 2^k ± 1，化为 t = b << k; a = t ± b
// 3. m = 2^k ± 2^r (k > r > 0)，化为 t = b << k; a = b << r; a = t ± a
// 如果 m 是负数，还需要额外加上 neg 指令，因此只有 1, 2 种情况需要优化
public class WeakenMult {
    public static void run(List<Mips> mipsList) {
        for (int i = 0; i < mipsList.size(); i++) {
            Mips mips = mipsList.get(i);
            String[] args = mips.args;
            // mul $t1, $t2, 123
            if (args[0].equals("mul") && isNumber(args[3])) {
                int inst = Integer.parseInt(args[3]);
                if (inst == 0) {
                    mipsList.set(i, new Mips(String.format("li %s, 0", args[1])));
                    continue;
                }

                List<String> codeList = new ArrayList<>();
                String target = args[1], mul = args[2];
                Integer k;
                Pair<Integer, Integer> pair; // (k, r)
                int abs = Math.abs(inst);
                if ((k = case1(abs)) != null) {
                    codeList.add(String.format("sll %s, %s, %d", target, mul, k));
                }
                else if ((k = case2Add(abs)) != null) {
                    codeList.add(String.format("sll $t9, %s, %d", mul, k));
                    codeList.add(String.format("add %s, $t9, %s", target, mul));
                }
                else if ((k = case2Sub(abs)) != null) {
                    codeList.add(String.format("sll $t9, %s, %d", mul, k));
                    codeList.add(String.format("sub %s, $t9, %s", target, mul));
                }
                else if (inst > 0 && (pair = case3Add(abs)) != null) {
                    codeList.add(String.format("sll $t9, %s, %d", mul, pair.first));
                    codeList.add(String.format("sll %s, %s, %d", target, mul, pair.second));
                    codeList.add(String.format("add %s, $t9, %s", target, target));
                }
                else if (inst > 0 && (pair = case3Sub(abs)) != null) {
                    codeList.add(String.format("sll $t9, %s, %d", mul, pair.first));
                    codeList.add(String.format("sll %s, %s, %d", target, mul, pair.second));
                    codeList.add(String.format("sub %s, $t9, %s", target, target));
                }
                else continue;

                if (inst < 0) codeList.add(String.format("sub %s, $zero, %s", target, target));

                mipsList.remove(i--);
                List<Mips> mulMipsList = codeList.stream()
                        .map(str -> new Mips(str))
                        .collect(Collectors.toList());
                mipsList.addAll(i, mulMipsList);
            }
        }
    }

    // 2^k? 返回 k
    private static Integer case1(int num) {
        int shift = 0;
        while ((1L << shift) < num) shift++;
        if ((1L << shift) == num) return shift;
        else return null;
    }

    // 2^k + 1? 返回 k
    private static Integer case2Add(int num) {
        return case1(num - 1);
    }

    // 2^k - 1? 返回 k
    private static Integer case2Sub(int num) {
        return case1(num + 1);
    }

    // 2^k + 2^r (k > r)? 返回 (k, r)
    private static Pair<Integer, Integer> case3Add(int num) {
        int shift = 0;
        int k = -1, r = -1;
        while ((num >> shift) > 0) {
            if ((num & (1L << shift)) > 0) {
                if (r == -1) r = shift;
                else if (k == -1) k = shift;
                else return null;
            }
            shift++;
        }
        if (k == -1) return null;
        else return new Pair<>(k, r);
    }

    // 2^k - 2^r (k > r)? 返回 (k, r)
    private static Pair<Integer, Integer> case3Sub(int num) {
        int shift = 0;
        while ((1L << shift) < num) shift++;
        Integer r = case1((int) ((1L << shift) - num));
        if (r != null) return new Pair<>(shift, r);
        else return null;
    }

    private static boolean isNumber(String str) {
        try {
            Integer.parseInt(str);
            return true;
        }
        catch (NumberFormatException e) {
            return false;
        }
    }
}

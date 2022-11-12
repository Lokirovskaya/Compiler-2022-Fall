package mips.mipsoptimizer;

import mips.Mips;
import util.NodeList;
import util.Pair;

import java.util.ArrayList;
import java.util.List;

// 乘法优化，由于 mul 指令只需要 4 ticks，因此只有以下三种情况优化有效：
// 对于 a = b * m (m > 0)
// 1. m = 2^k，化为 a = b << k
// 2. m = 2^k ± 1，化为 t = b << k; a = t ± b
// 3. m = 2^k ± 2^r (k > r > 0)，化为 t = b << k; a = b << r; a = t ± a
// 如果 m 是负数，还需要额外加上 neg 指令，因此只有 1, 2 种情况需要优化
public class WeakenMult {
    public static void run(NodeList<Mips> inter) {
        inter.forEachNode(p -> {
            String[] args = p.get().args;
            // mul $t1, $t2, 123
            if (args[0].equals("mul") && isNumber(args[3])) {
                int inst = Integer.parseInt(args[3]);
                if (inst == 0) {
                    p.set(new Mips(String.format("li %s, 0", args[1])));
                    return;
                }

                List<String> mipsList = new ArrayList<>();
                String target = args[1], mul = args[2];
                Integer k;
                Pair<Integer, Integer> pair; // (k, r)
                int abs = Math.abs(inst);
                if ((k = case1(abs)) != null) {
                    mipsList.add(String.format("sll %s, %s, %d", target, mul, k));
                }
                else if ((k = case2Add(abs)) != null) {
                    mipsList.add(String.format("sll $t9, %s, %d", mul, k));
                    mipsList.add(String.format("add %s, $t9, %s", target, mul));
                }
                else if ((k = case2Sub(abs)) != null) {
                    mipsList.add(String.format("sll $t9, %s, %d", mul, k));
                    mipsList.add(String.format("sub %s, $t9, %s", target, mul));
                }
                else if (inst > 0 && (pair = case3Add(abs)) != null) {
                    mipsList.add(String.format("sll $t9, %s, %d", mul, pair.first));
                    mipsList.add(String.format("sll %s, %s, %d", target, mul, pair.second));
                    mipsList.add(String.format("add %s, $t9, %s", target, target));
                }
                else if (inst > 0 && (pair = case3Sub(abs)) != null) {
                    mipsList.add(String.format("sll $t9, %s, %d", mul, pair.first));
                    mipsList.add(String.format("sll %s, %s, %d", target, mul, pair.second));
                    mipsList.add(String.format("sub %s, $t9, %s", target, target));
                }
                else return;

                if (inst < 0) mipsList.add(String.format("sub %s, $zero, %s", target, target));

                for (int i = mipsList.size() - 1; i >= 0; i--) {
                    p.insertNext(new Mips(mipsList.get(i)));
                }
                p.delete();
            }
        });
    }

    // 2^k? 返回 k
    private static Integer case1(int num) {
        int shift = 0;
        while ((1 << shift) < num) shift++;
        if ((1 << shift) == num) return shift;
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
            if ((num & (1 << shift)) > 0) {
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
        while ((1 << shift) < num) shift++;
        Integer r = case1((1 << shift) - num);
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

package optimizer.mipsoptimizer;

import mips.Mips;

import java.util.ArrayList;
import java.util.List;

public class ConvertMod {
    public static void run(List<Mips> mipsList) {
        // rem $target, $modReg, inst
        // 转化为
        // $target = $modReg - $modReg/inst * inst
        for (int i = 0; i < mipsList.size(); i++) {
            String[] args = mipsList.get(i).args;
            if (!(args[0].equals("rem") && isNumber(args[3]))) continue;

            int inst = Integer.parseInt(args[3]);
            String target = args[1];
            String modReg = args[2];

            mipsList.remove(i--);
            List<Mips> modMipsList = new ArrayList<>();
            modMipsList.add(new Mips(String.format("div $t9, %s, %d", modReg, inst)));
            modMipsList.add(new Mips(String.format("mul $t9, $t9, %d", inst)));
            modMipsList.add(new Mips(String.format("sub %s, %s, $t9", target, modReg)));
            mipsList.addAll(i + 1, modMipsList);
        }
    }

    private static boolean isNumber(String str) {
        return str != null && str.matches("^[+-]?[0-9]+$");
    }
}

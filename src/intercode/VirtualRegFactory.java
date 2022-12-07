package intercode;

import intercode.Operand.VirtualReg;

public class VirtualRegFactory {
    private static int regIdx = 1;

    static void init() {
        regIdx = 1;
    }

    public static VirtualReg newReg() {
        return new VirtualReg(regIdx++);
    }

    public static VirtualReg newCopyReg(VirtualReg sourceReg) {
        VirtualReg copyReg = newReg();
        copyReg.isGlobal = sourceReg.isGlobal;
        copyReg.isAddr = sourceReg.isAddr;
        copyReg.isParam = sourceReg.isParam;
        return copyReg;
    }
}

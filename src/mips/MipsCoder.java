package mips;

import intercode.InterCode;
import intercode.Operand;
import intercode.Operand.InstNumber;
import intercode.Operand.VirtualReg;
import intercode.Quaternion;
import util.Pair;
import util.Wrap;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Map;

public class MipsCoder {
    private final InterCode inter;
    StringBuilder mips = new StringBuilder();
    Map<VirtualReg, Integer> vregOffsetMap;
    Map<String, Integer> funcSizeMap;

    public MipsCoder(InterCode inter) {
        this.inter = inter;
    }

    public void generateMips() {
        Pair<Map<VirtualReg, Integer>, Map<String, Integer>> pair = Allocator.alloc(inter);
        vregOffsetMap = pair.first;
        funcSizeMap = pair.second;
        generate();
    }

    public void output(String filename) throws IOException {
        Files.write(Paths.get(filename), mips.toString().getBytes(StandardCharsets.UTF_8));
    }

    private int getFuncSize(String func) {
        return funcSizeMap.get(func);
    }

    private void addMips(String format, Object... args) {
        mips.append(String.format(format, args)).append('\n');
    }

    // 涉及到虚寄存器的语句，对未分配的虚寄存器进行 lw/sw
    // 约定，format 中 #t 表示 target，#x1 表示 x1，#x2 表示 x2，#label 表示 label
    private void addRegMips(String format, Quaternion quater) {
        String regTarget = "", regX1 = "", regX2 = "", label = "";
        boolean saveTarget = false;
        if (quater.target != null) {
            if (quater.target.realReg >= 0) regTarget = MipsUtil.getRegName(quater.target.realReg);
            else {
                regTarget = "$t8";
                saveTarget = true;
            }
        }
        if (quater.x1 instanceof Operand.InstNumber) regX1 = quater.x1.toString();
        else if (quater.x1 instanceof Operand.VirtualReg) {
            if (((Operand.VirtualReg) quater.x1).realReg >= 0)
                regX1 = MipsUtil.getRegName(((Operand.VirtualReg) quater.x1).realReg);
            else {
                regX1 = "$t8";
                addMips("lw $t8, %d($sp)", vregOffsetMap.get(quater.x1));
            }
        }
        if (quater.x2 instanceof Operand.InstNumber) regX2 = quater.x2.toString();
        else if (quater.x2 instanceof Operand.VirtualReg) {
            if (((Operand.VirtualReg) quater.x2).realReg >= 0)
                regX2 = MipsUtil.getRegName(((Operand.VirtualReg) quater.x2).realReg);
            else {
                regX2 = "$t8";
                addMips("lw $t8, %d($sp)", vregOffsetMap.get(quater.x2));
            }
        }
        if (quater.label != null) label = quater.label.toString();

        addMips(format.replace("#t", regTarget)
                .replace("#x1", regX1)
                .replace("#x2", regX2)
                .replace("#label", label));
        if (saveTarget) addMips("sw %s, %d($sp)", regTarget, vregOffsetMap.get(quater.target));
    }

    private void generate() {
        addMips(".text");

        Wrap<String> curFunc = new Wrap<>(null);
        inter.forEach(p -> {
            switch (p.get().op) {
                case FUNC:
                    curFunc.set(p.get().label.name);
                    addMips("func_%s:", curFunc.get());
                    addMips("addi $sp, $sp, -%d", getFuncSize(curFunc.get()));
                    break;
                case END_FUNC:
                    addMips("addi $sp, $sp, %d", getFuncSize(curFunc.get()));
                    break;
                case RETURN:
                    addMips("addi $sp, $sp, %d", getFuncSize(curFunc.get()));
                    addMips("jr $ra");
                    break;
                case CALL:
                    if (curFunc.get() != null)
                        addMips("sw $ra, %d($sp)", getFuncSize(curFunc.get()) - 4);
                    addMips("jal func_%s", p.get().label.name);
                    if (curFunc.get() != null)
                        addMips("lw $ra, %d($sp)", getFuncSize(curFunc.get()) - 4);
                    break;
                case EXIT:
                    addMips("li $v0, 10");
                    addMips("syscall");
                    break;
                case LABEL:
                    addMips("%s:", p.get().label.name);
                    break;
                case ADD:
                    if (p.get().x1 instanceof VirtualReg && p.get().x2 instanceof VirtualReg)
                        addRegMips("add #t, #x1, #x2", p.get());
                    else if (p.get().x1 instanceof VirtualReg && p.get().x2 instanceof InstNumber)
                        addRegMips("addi #t, #x1, #x2", p.get());
                    else if (p.get().x1 instanceof InstNumber && p.get().x2 instanceof VirtualReg)
                        addRegMips("addi #t, #x2, #x1", p.get());
                    break;

            }
        });
    }
}

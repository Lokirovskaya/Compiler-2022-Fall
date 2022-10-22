package mips;

import intercode.InterCode;
import intercode.Operand;
import intercode.Operand.InstNumber;
import intercode.Operand.VirtualReg;
import intercode.Quaternion;
import util.NodeList;
import util.Pair;
import util.Wrap;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Map;

public class MipsCoder {
    private final InterCode inter;
    NodeList<String> mips = new NodeList<>();
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
        MipsUtil.optimize(mips);
    }

    public void output(String filename) throws IOException {
        StringBuilder result = new StringBuilder();
        mips.forEach(p -> result.append(p.get()).append('\n'));
        Files.write(Paths.get(filename), result.toString().getBytes(StandardCharsets.UTF_8));
    }

    private int getFuncSize(String func) {
        return funcSizeMap.get(func);
    }

    private void addMips(String format, Object... args) {
        assert !format.contains("@");
        mips.addLast(String.format(format, args));
    }

    // 涉及到虚寄存器的语句，对未分配的虚寄存器进行 lw/sw
    // 约定，format 中 @t 表示 target，@x1 表示 x1，@x2 表示 x2，@label 表示 label
    private void addRegMips(String format, Quaternion quater) {
        String regTarget = "@t", regX1 = "@x1", regX2 = "@x2", label = "@label";
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
                regX2 = "$t9";
                addMips("lw $t9, %d($sp)", vregOffsetMap.get(quater.x2));
            }
        }
        if (quater.label != null) label = quater.label.toString();

        addMips(format.replace("@t", regTarget)
                .replace("@x1", regX1)
                .replace("@x2", regX2)
                .replace("@label", label));
        if (saveTarget) addMips("sw %s, %d($sp)", regTarget, vregOffsetMap.get(quater.target));
    }

    // 翻译时保证：
    // 不会全部操作数为立即数 optimizer.MergeInst
    // 不会 x1 为立即数，x2 为寄存器 optimizer.SwapOperand
    private void generate() {
        addMips(".text");

        Wrap<String> curFunc = new Wrap<>(null);
        inter.forEach(p -> {
            switch (p.get().op) {
                case FUNC:
                    curFunc.set(p.get().label.name);
                    addMips("func_%s:", curFunc.get());
                    addMips("add $sp, $sp, -%d", getFuncSize(curFunc.get()));
                    break;
                case END_FUNC:
                    addMips("add $sp, $sp, %d", getFuncSize(curFunc.get()));
                    break;
                case RETURN:
                    addMips("add $sp, $sp, %d", getFuncSize(curFunc.get()));
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
                case SET:
                    if (p.get().x1 instanceof VirtualReg)
                        addRegMips("move @t, @x1", p.get());
                    else
                        addRegMips("li @t, @x1", p.get());
                    break;
                case ADD:
                    addRegMips("add @t, @x1, @x2", p.get());
                    break;
                case SUB:
                    addRegMips("sub @t, @x1, @x2", p.get());
                    break;
                case MULT:
                    addRegMips("mul @t, @x1, @x2", p.get());
                    break;
                case DIV:
                    addRegMips("div @x1, @x2", p.get());
                    addRegMips("mflo @t", p.get());
                    break;
                case MOD:
                    addRegMips("div @x1, @x2", p.get());
                    addRegMips("mfhi @t", p.get());
                    break;
                case NEG:
                    addRegMips("sub @t, $zero, @x1", p.get());
                    break;
                case NOT:
                    break;
                case EQ:
                    addRegMips("seq @t, @x1, @x2", p.get());
                    break;
                case NOT_EQ:
                    addRegMips("sne @t, @x1, @x2", p.get());
                    break;
                case LESS:
                    addRegMips("slt @t, @x1, @x2", p.get());
                    break;
                case LESS_EQ:
                    addRegMips("sle @t1, @x1, @x2", p.get());
                    break;
                case GREATER:
                    addRegMips("sgt @t, @x1, @x2", p.get());
                    break;
                case GREATER_EQ:
                    addRegMips("sge @t, @x1, @x2", p.get());
                    break;
                case GOTO:
                    addRegMips("j @label", p.get());
                    break;
                case IF:
                    addRegMips("bne @x1, $zero, @label", p.get());
                    break;
                case IF_NOT:
                    addRegMips("beq @x1, $zero, @label", p.get());
                    break;
                case IF_EQ:
                    addRegMips("beq @x1, @x2, @label", p.get());
                    break;
                case IF_NOT_EQ:
                    addRegMips("bne @x1, @x2, @label", p.get());
                    break;
                case IF_LESS:
                    if (isZero(p.get().x2))
                        addRegMips("bltz @x1, @label", p.get());
                    else
                        addRegMips("blt @x1, @x2, @label", p.get());
                    break;
                case IF_LESS_EQ:
                    if (isZero(p.get().x2))
                        addRegMips("blez @x1, @label", p.get());
                    else
                        addRegMips("ble @x1, @x2, @label", p.get());
                    break;
                case IF_GREATER:
                    if (isZero(p.get().x2))
                        addRegMips("bgtz @x1, @label", p.get());
                    else
                        addRegMips("bgt @x1, @x2, @label", p.get());
                    break;
                case IF_GREATER_EQ:
                    if (isZero(p.get().x2))
                        addRegMips("bgez @x1, @label", p.get());
                    else
                        addRegMips("bge @x1, @x2, @label", p.get());
                    break;
                case GETINT:
                    addMips("li $v0, 5");
                    addMips("syscall");
                    addRegMips("move @t, $v0", p.get());
                    break;
                case PRINT_STR:
                    //todo
                    break;
                case PRINT_CHAR:
                    addMips("li $v0, 11");
                    if (p.get().x1 instanceof VirtualReg)
                        addRegMips("move $a0, @x1", p.get());
                    else
                        addRegMips("li $a0, @x1", p.get());
                    addMips("syscall");
                    break;
                case PRINT_INT:
                    addMips("li $v0, 1");
                    if (p.get().x1 instanceof VirtualReg)
                        addRegMips("move $a0, @x1", p.get());
                    else
                        addRegMips("li $a0, @x1", p.get());
                    addMips("syscall");
                    break;
            }
        });
    }

    private boolean isZero(Operand x) {
        if (x instanceof VirtualReg) return ((VirtualReg) x).realReg == 0;
        else return ((InstNumber) x).number == 0;
    }
}

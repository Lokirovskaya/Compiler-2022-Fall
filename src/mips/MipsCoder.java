package mips;

import intercode.InterCode;
import intercode.Operand;
import intercode.Operand.InstNumber;
import intercode.Operand.VirtualReg;
import intercode.Quaternion;
import intercode.Quaternion.OperatorType;
import util.NodeList;
import util.Wrap;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;

public class MipsCoder {
    private final InterCode inter;
    private final NodeList<String> mips = new NodeList<>();
    private AllocationInfo allocInfo;

    public MipsCoder(InterCode inter) {
        this.inter = inter;
    }

    public void generateMips() {
        this.allocInfo = Allocator.alloc(inter);
        System.out.println(allocInfo);
        generate();
        MipsUtil.optimize(mips);
    }

    public void output(String filename) throws IOException {
        StringBuilder result = new StringBuilder();
        mips.forEach(p -> result.append(p.get()).append('\n'));
        Files.write(Paths.get(filename), result.toString().getBytes(StandardCharsets.UTF_8));
    }

    private void addMips(String format, Object... args) {
        assert !format.contains("@");
        mips.addLast(String.format(format, args));
    }

    // 涉及到虚寄存器的语句，对未分配的虚寄存器进行 lw/sw，对已分配的虚寄存器进行直接翻译
    // 约定，format 中 @t 表示 target，@x1 表示 x1，@x2 表示 x2，@label 表示 label
    private void addRegMips(String format, Quaternion quater) {
        String regTarget = "@t", regX1 = "@x1", regX2 = "@x2", label = "@label";
        boolean saveTarget = false;
        if (quater.target != null) {
            if (quater.target.realReg >= 0)
                regTarget = MipsUtil.getRegName(quater.target.realReg);
            else {
                regTarget = "$t8";
                saveTarget = true;
            }
        }
        if (quater.x1 instanceof InstNumber)
            regX1 = quater.x1.toString();
        else if (quater.x1 instanceof VirtualReg) {
            if (((VirtualReg) quater.x1).realReg >= 0)
                regX1 = MipsUtil.getRegName(((VirtualReg) quater.x1).realReg);
            else {
                regX1 = "$t8";
                addMips("lw $t8, %d($sp)", allocInfo.getVregOffset((VirtualReg) quater.x1));
            }
        }
        if (quater.x2 instanceof InstNumber)
            regX2 = quater.x2.toString();
        else if (quater.x2 instanceof VirtualReg) {
            if (((VirtualReg) quater.x2).realReg >= 0)
                regX2 = MipsUtil.getRegName(((VirtualReg) quater.x2).realReg);
            else {
                regX2 = "$t9";
                addMips("lw $t9, %d($sp)", allocInfo.getVregOffset((VirtualReg) quater.x2));
            }
        }
        if (quater.label != null) label = quater.label.toString();

        addMips(format.replace("@t", regTarget)
                .replace("@x1", regX1)
                .replace("@x2", regX2)
                .replace("@label", label));
        if (saveTarget) addMips("sw %s, %d($sp)", regTarget, allocInfo.getVregOffset(quater.target));
    }

    // 翻译时保证：
    // 不会全部操作数为立即数
    // 对于 x1, x2 可交换的命令，不会 x1 为立即数，x2 为寄存器
    private void generate() {
        addMips(".text");

        Wrap<Integer> curCallParamIdx = new Wrap<>(0);
        Wrap<String> curCallFuncName = new Wrap<>(null);
        inter.forEach(p -> {
            switch (p.get().op) {
                case FUNC:
                    addMips("jr $ra");
                    addMips("func_%s:", p.get().label.name);
                    break;
                case RETURN:
                    addMips("jr $ra");
                    break;
                case PARAM:
                    // 什么都不用做，因为参数已经由调用者放到了记录好的位置
                    break;
                // 对于函数的调用者：
                // 1. 存放当前上下文的 $ra 到 0($sp) 位置
                // 2. 依照当前 $sp 和目标函数参数的 offset，减去一整个目标函数的调用栈大小，存放目标函数需要的参数
                // 3. 按照目标函数的调用栈大小，向小地址移动 $sp
                // 4. jal
                // 5. 按照目标函数的调用栈大小，恢复 $sp
                // 6. 恢复 $ra
                case CALL:
                    curCallFuncName.set(p.get().label.name);
                    curCallParamIdx.set(0);
                    addMips("sw $ra, 0($sp)");
                    break;
                case PUSH:
                    VirtualReg param = allocInfo.getFuncParam(curCallFuncName.get(), curCallParamIdx.get());
                    Integer offset = allocInfo.getVregOffset(param);
                    if (offset == null) {
                        assert param.realReg >= 0;
                        addRegMips(String.format("move %s, @x1", MipsUtil.getRegName(param.realReg)), p.get());
                    }
                    else {
                        int curCallFuncSize = allocInfo.getFuncSize(curCallFuncName.get());
                        if (p.get().x1 instanceof InstNumber) {
                            addMips("li $t8, %d", ((InstNumber) p.get().x1).number);
                            addMips("sw $t8, %d($sp)", offset - curCallFuncSize);
                        }
                        else addRegMips(String.format("sw @x1, %d($sp)", offset - curCallFuncSize), p.get());
                    }
                    curCallParamIdx.set(curCallParamIdx.get() + 1);
                    break;
                case END_CALL:
                    addMips("add $sp, $sp, -%d", allocInfo.getFuncSize(curCallFuncName.get()));
                    if (curCallFuncName.get().equals("main"))
                        addMips("jal func_main");
                    else {
                        addMips("jal func_%s", curCallFuncName.get());
                        addMips("add $sp, $sp, %d", allocInfo.getFuncSize(curCallFuncName.get()));
                        addMips("lw $ra, 0($sp)");
                    }
                    curCallParamIdx.set(0);
                    curCallFuncName.set(null);
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
                    if (p.get().x1 instanceof InstNumber) {
                        addMips("li $t8, %d", ((InstNumber) p.get().x1).number);
                        addRegMips("sub @t, $t8, @x2", p.get());
                    }
                    else {
                        if (p.get().x2 instanceof VirtualReg)
                            addRegMips("sub @t, @x1, @x2", p.get());
                        else {
                            // 不要使用 subi 命令，它是伪指令，并且会被翻译成两条语句
                            p.get().op = OperatorType.ADD;
                            p.get().x2 = new InstNumber(-((InstNumber) p.get().x2).number);
                            addRegMips("add @t, @x1, @x2", p.get());
                        }
                    }
                    break;
                case MULT:
                    addRegMips("mul @t, @x1, @x2", p.get());
                    break;
                case DIV:
                    if (p.get().x1 instanceof InstNumber) {
                        addMips("li $t8, %d", ((InstNumber) p.get().x1).number);
                        addRegMips("div $t8, @x2", p.get());
                    }
                    else {
                        addRegMips("div @x1, @x2", p.get());
                    }
                    addRegMips("mflo @t", p.get());
                    break;
                case MOD:
                    if (p.get().x1 instanceof InstNumber) {
                        addMips("li $t8, %d", ((InstNumber) p.get().x1).number);
                        addRegMips("div $t8, @x2", p.get());
                    }
                    else {
                        addRegMips("div @x1, @x2", p.get());
                    }
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
                    if (p.get().x1 instanceof VirtualReg)
                        addRegMips("move $a0, @x1", p.get());
                    else
                        addRegMips("li $a0, @x1", p.get());
                    addMips("li $v0, 11");
                    addMips("syscall");
                    break;
                case PRINT_INT:
                    if (p.get().x1 instanceof VirtualReg)
                        addRegMips("move $a0, @x1", p.get());
                    else
                        addRegMips("li $a0, @x1", p.get());
                    addMips("li $v0, 1");
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

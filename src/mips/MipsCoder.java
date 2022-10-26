package mips;

import intercode.InterCode;
import intercode.Operand;
import intercode.Operand.InstNumber;
import intercode.Operand.VirtualReg;
import intercode.Quaternion;
import intercode.Quaternion.OperatorType;
import util.NodeList;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Stack;

import static intercode.Quaternion.OperatorType.*;

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

    // 获取 vreg 对应的 reg；如果没有预先分配，就将 tempReg 分配给它
    private String getReg(VirtualReg vreg, String tempReg) {
        if (vreg.realReg >= 0) return MipsUtil.getRegName(vreg.realReg);
        if (vreg.isGlobal)
            addMips("lw %s, %d($gp)", tempReg, allocInfo.getVregOffset(vreg));
        else
            addMips("lw %s, %d($sp)", tempReg, allocInfo.getVregOffset(vreg));
        return tempReg;
    }

    // 涉及到虚寄存器的语句，对未分配的虚寄存器进行 lw/sw，对已分配的虚寄存器进行直接翻译
    // format 中，@t 表示 target，@x1 表示 x1，@x2 表示 x2，@label 表示 label
    // @rx1 表示必须是寄存器的 x1，若 x1 是立即数，会额外添加 li 指令；@rx2 同理
    private void addRegMips(String format, Quaternion quater) {
        String tReg, x1RegInst, x2RegInst, x1Reg, x2Reg, label;
        tReg = x1RegInst = x2RegInst = x1Reg = x2Reg = label = "";
        if (format.contains("@t")) {
            tReg = (quater.target.realReg >= 0) ? MipsUtil.getRegName(quater.target.realReg) : "$t8";
        }
        if (format.contains("@x1")) {
            if (isZero(quater.x1))
                x1RegInst = "$zero";
            else if (quater.x1 instanceof VirtualReg)
                x1RegInst = getReg((VirtualReg) quater.x1, "$t8");
            else
                x1RegInst = String.valueOf(((InstNumber) quater.x1).number);
        }
        if (format.contains("@x2")) {
            if (isZero(quater.x2))
                x2RegInst = "$zero";
            else if (quater.x2 instanceof VirtualReg)
                x2RegInst = getReg((VirtualReg) quater.x2, "$t9");
            else
                x2RegInst = String.valueOf(((InstNumber) quater.x2).number);
        }
        if (format.contains("@rx1")) {
            if (isZero(quater.x1))
                x1Reg = "$zero";
            else if (quater.x1 instanceof VirtualReg)
                x1Reg = getReg((VirtualReg) quater.x1, "$t8");
            else {
                addMips("li $t8, %d", ((InstNumber) quater.x1).number);
                x1Reg = "$t8";
            }
        }
        if (format.contains("@rx2")) {
            if (isZero(quater.x2))
                x2Reg = "$zero";
            else if (quater.x2 instanceof VirtualReg)
                x2Reg = getReg((VirtualReg) quater.x2, "$t9");
            else {
                addMips("li $t9, %d", ((InstNumber) quater.x2).number);
                x2Reg = "$t9";
            }
        }
        if (format.contains("@label")) {
            label = quater.label.name;
        }
        addMips(format.replace("@t", tReg)
                .replace("@x1", x1RegInst)
                .replace("@x2", x2RegInst)
                .replace("@rx1", x1Reg)
                .replace("@rx2", x2Reg)
                .replace("@label", label));
        if (format.contains("@t")) {
            if (quater.target.realReg < 0) {
                if (quater.target.isGlobal)
                    addMips("sw %s, %d($gp)", tReg, allocInfo.getVregOffset(quater.target));
                else
                    addMips("sw %s, %d($sp)", tReg, allocInfo.getVregOffset(quater.target));
            }
        }
    }

    private void generate() {
        if (inter.getFirst().op == STR_DECLARE) addMips(".data");
        else addMips(".text");

        Stack<Operand> paramStack = new Stack<>();
        inter.forEach(p -> {
            OperatorType op = p.get().op;
//            addMips("# %s", op.name());
            switch (op) {
                case STR_DECLARE:
                    addMips("str_%d: .asciiz \"%s\"", ((InstNumber) p.get().x1).number, p.get().label);
                    if (p.get(1).op != STR_DECLARE) addMips(".text");
                    break;
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
                case PUSH:
                    paramStack.push(p.get().x1);
                    break;
                // 对于函数的调用者：
                // 1. 依照当前 $sp 和目标函数参数的 offset，减去一整个目标函数的调用栈大小，存放目标函数需要的参数
                // 2. 存放当前上下文的 $ra 到 0($sp) 位置
                // 3. 按照目标函数的调用栈大小，向小地址移动 $sp
                // 4. jal
                // 5. 按照目标函数的调用栈大小，恢复 $sp
                // 6. 恢复 $ra
                case CALL: {
                    String funcName = p.get().label.name;
                    int paramCount = allocInfo.getFuncParamCount(funcName);
                    for (int i = 0; i < paramCount; i++) {
                        VirtualReg paramDef = allocInfo.getFuncParam(funcName, paramCount - i - 1); // 形参（保留在栈上还是寄存器中）
                        Operand paramCall = paramStack.pop(); // 实参（是 vreg 还是立即数）

                        // 建立 实参 -> 形参 的传递
                        if (paramDef.realReg >= 0) {
                            String paramDefReg = MipsUtil.getRegName(paramDef.realReg);
                            if (paramCall instanceof InstNumber) {
                                addMips("li %s, %d", paramDefReg, ((InstNumber) paramCall).number);
                            }
                            else if (paramCall instanceof VirtualReg) {
                                String paramCallReg = getReg((VirtualReg) paramCall, "$t8");
                                addMips("move %s, %s", paramDefReg, paramCallReg);
                            }
                        }
                        else {
                            int paramDefOffset = allocInfo.getVregOffset(paramDef) - allocInfo.getFuncSize(funcName);
                            if (paramCall instanceof InstNumber) {
                                addMips("li $t8, %d", ((InstNumber) paramCall).number);
                                addMips("sw $t8, %d($sp)", paramDefOffset);
                            }
                            else if (paramCall instanceof VirtualReg) {
                                String paramCallReg = getReg((VirtualReg) paramCall, "$t8");
                                addMips("sw %s, %d($sp)", paramCallReg, paramDefOffset);
                            }
                        }
                    }
                    if (funcName.equals("main")) {
                        addMips("add $sp, $sp, -%d", allocInfo.getFuncSize(funcName));
                        addMips("jal func_main");
                    }
                    else {
                        addMips("sw $ra, 0($sp)");
                        addMips("add $sp, $sp, -%d", allocInfo.getFuncSize(funcName));
                        addMips("jal func_%s", funcName);
                        addMips("add $sp, $sp, %d", allocInfo.getFuncSize(funcName));
                        addMips("lw $ra, 0($sp)");
                    }
                    break;
                }
                case EXIT:
                    addMips("li $v0, 10");
                    addMips("syscall");
                    break;
                case LABEL:
                    addMips("%s:", p.get().label.name);
                    break;
                case SET:
                    if (p.get().x1 instanceof VirtualReg)
                        addRegMips("move @t, @rx1", p.get());
                    else
                        addRegMips("li @t, @x1", p.get());
                    break;
                case ALLOC:
                    if (p.get().target.isGlobal)
                        addRegMips(String.format("add @t, $gp, %d", allocInfo.getVregOffset(p.get().target) + 4), p.get());
                    else
                        addRegMips(String.format("add @t, $sp, %d", allocInfo.getVregOffset(p.get().target) + 4), p.get());
                    break;
                case GET_ARRAY: {
                    // @t = @x1[@x2]
                    if (p.get().x2 instanceof InstNumber) {
                        addRegMips(String.format("lw @t, %d(@rx1)", ((InstNumber) p.get().x2).number * 4), p.get());
                    }
                    else {
                        addRegMips("sll $t9, @rx2, 2", p.get());
                        addRegMips("add $t9, $t9, @x1", p.get());
                        addRegMips("lw @t, 0($t9)", p.get());
                    }
                    break;
                }
                case SET_ARRAY: {
                    // @t[@x1] = @x2，@t 在这里不会被改变，因此不要使用含有 @t 的 addRegMips
                    // 最终形式为 sw valueReg, offsetReg(baseReg)
                    String baseReg = getReg(p.get().target, "$t8");
                    String offsetReg;
                    if (p.get().x1 instanceof InstNumber) {
                        offsetReg = String.valueOf(((InstNumber) p.get().x1).number * 4);
                    }
                    else {
                        String indexReg = getReg((VirtualReg) p.get().x1, "$t9");
                        addMips("sll %s, %s, 2", indexReg, indexReg);
                        addMips("add %s, %s, %s", baseReg, baseReg, indexReg);
                        offsetReg = "0";
                    }
                    addRegMips(String.format("sw @rx2, %s(%s)", offsetReg, baseReg), p.get());
                    break;
                }
                case ADD_ADDR:
                    assert p.get().target.isAddr;
                    assert p.get().x1 instanceof VirtualReg && ((VirtualReg) p.get().x1).isAddr;
                    if (p.get().x2 instanceof InstNumber) {
                        addRegMips(String.format("add @t, @rx1, %d", ((InstNumber) p.get().x2).number * 4), p.get());
                    }
                    else if (p.get().x2 instanceof VirtualReg) {
                        addRegMips("sll $t9, @rx2, 2", p.get());
                        addRegMips("add @t, @rx1, $t9", p.get());
                    }
                    break;
                case ADD:
                    addRegMips("add @t, @rx1, @x2", p.get());
                    break;
                case SUB:
                    if (p.get().x2 instanceof VirtualReg)
                        addRegMips("sub @t, @rx1, @x2", p.get());
                    else
                        addRegMips(String.format("add @t, @rx1, %d", -((InstNumber) p.get().x2).number), p.get());
                    break;
                case MULT:
                    addRegMips("mul @t, @rx1, @x2", p.get());
                    break;
                case DIV:
                    addRegMips("div @rx1, @rx2", p.get());
                    addRegMips("mflo @t", p.get());
                    break;
                case MOD:
                    addRegMips("div @rx1, @rx2", p.get());
                    addRegMips("mfhi @t", p.get());
                    break;
                case NEG:
                    addRegMips("sub @t, $zero, @x1", p.get());
                    break;
                case SHIFT_LEFT:
                    addRegMips("sll @t, @rx1, @x2", p.get());
                    break;
                case SHIFT_RIGHT:
                    addRegMips("srl @t, @rx1, @x2", p.get());
                    break;
                case NOT:
                    addRegMips("xor @t, @t, 1", p.get());
                    break;
                case EQ:
                    addRegMips("seq @t, @rx1, @x2", p.get());
                    break;
                case NOT_EQ:
                    addRegMips("sne @t, @rx1, @x2", p.get());
                    break;
                case LESS:
                    if (p.get().x2 instanceof InstNumber)
                        addRegMips("slti @t, @rx1, @x2", p.get());
                    else
                        addRegMips("slt @t, @rx1, @x2", p.get());
                    break;
                case LESS_EQ:
                    addRegMips("sle @t, @rx1, @x2", p.get());
                    break;
                case GREATER:
                    addRegMips("sgt @t, @rx1, @x2", p.get());
                    break;
                case GREATER_EQ:
                    addRegMips("sge @t, @rx1, @x2", p.get());
                    break;
                case GOTO:
                    addRegMips("j @label", p.get());
                    break;
                case IF:
                    addRegMips("bne @rx1, $zero, @label", p.get());
                    break;
                case IF_NOT:
                    addRegMips("beq @rx1, $zero, @label", p.get());
                    break;
                case IF_EQ:
                    if (isZero(p.get().x2))
                        addRegMips("beq @rx1, $zero, @label", p.get());
                    else
                        addRegMips("beq @rx1, @x2, @label", p.get());
                    break;
                case IF_NOT_EQ:
                    if (isZero(p.get().x2))
                        addRegMips("bne @rx1, $zero, @label", p.get());
                    else
                        addRegMips("bne @rx1, @x2, @label", p.get());
                    break;
                case IF_LESS:
                    if (isZero(p.get().x2))
                        addRegMips("bltz @rx1, @label", p.get());
                    else
                        addRegMips("blt @rx1, @x2, @label", p.get());
                    break;
                case IF_LESS_EQ:
                    if (isZero(p.get().x2))
                        addRegMips("blez @rx1, @label", p.get());
                    else
                        addRegMips("ble @rx1, @x2, @label", p.get());
                    break;
                case IF_GREATER:
                    if (isZero(p.get().x2))
                        addRegMips("bgtz @rx1, @label", p.get());
                    else
                        addRegMips("bgt @rx1, @x2, @label", p.get());
                    break;
                case IF_GREATER_EQ:
                    if (isZero(p.get().x2))
                        addRegMips("bgez @rx1, @label", p.get());
                    else
                        addRegMips("bge @rx1, @x2, @label", p.get());
                    break;
                case GETINT:
                    addMips("li $v0, 5");
                    addMips("syscall");
                    addRegMips("move @t, $v0", p.get());
                    break;
                case PRINT_STR:
                    addMips("la $a0, str_%d", ((InstNumber) p.get().x1).number);
                    addMips("li $v0, 4");
                    addMips("syscall");
                    break;
                case PRINT_CHAR:
                    if (p.get().x1 instanceof VirtualReg)
                        addRegMips("move $a0, @rx1", p.get());
                    else
                        addRegMips("li $a0, @x1", p.get());
                    addMips("li $v0, 11");
                    addMips("syscall");
                    break;
                case PRINT_INT:
                    if (p.get().x1 instanceof VirtualReg)
                        addRegMips("move $a0, @rx1", p.get());
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

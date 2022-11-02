package mips;

import intercode.InterCode;
import intercode.Label;
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
    private final NodeList<Mips> mipsList = new NodeList<>();
    private AllocationInfo allocInfo;

    public MipsCoder(InterCode inter) {
        this.inter = inter;
    }

    public void generateMips() {
        this.allocInfo = Allocator.alloc(inter);
//        this.allocInfo.printAllocInfo();
        generate();
        MipsOptimizer.optimize(mipsList);
    }

    public void output(String filename) throws IOException {
        StringBuilder result = new StringBuilder();
        mipsList.forEach(p -> result.append(p.get().code).append('\n'));
        Files.write(Paths.get(filename), result.toString().getBytes(StandardCharsets.UTF_8));
    }

    private static final String[] regNames = {"zero", "at", "v0", "v1", "a0", "a1", "a2", "a3", "t0", "t1", "t2", "t3", "t4", "t5", "t6", "t7", "s0", "s1", "s2", "s3", "s4", "s5", "s6", "s7", "t8", "t9", "k0", "k1", "gp", "sp", "fp", "ra"};

    private static String getRegName(int reg) {
        return "$" + regNames[reg];
    }

    private void addMips(String format, Object... args) {
        assert !format.contains("@");
        mipsList.addLast(new Mips(String.format(format, args)));
    }

    // 获取 vreg 对应的 reg；如果 vreg 存放在栈上，就从栈上加载到 defaultReg 中
    private String loadVregToReg(VirtualReg vreg, String defaultReg) {
        if (vreg.realReg >= 0) return getRegName(vreg.realReg);
        if (vreg.isGlobal)
            addMips("lw %s, %d($gp)", defaultReg, allocInfo.getVregOffset(vreg));
        else
            addMips("lw %s, %d($sp)", defaultReg, allocInfo.getVregOffset(vreg));
        return defaultReg;
    }

    // 涉及到虚寄存器的语句，对未分配的虚寄存器进行 lw/sw，对已分配的虚寄存器进行直接翻译
    // format 中，@t 表示 target，@x1 表示 x1，@x2 表示 x2，@label 表示 label
    // @rx1 表示必须是寄存器的 x1，若 x1 是立即数，会额外添加 li 指令；@rx2 同理
    private void addRegMips(String format, Quaternion quater) {
        String tReg, x1RegInst, x2RegInst, x1Reg, x2Reg, label;
        tReg = x1RegInst = x2RegInst = x1Reg = x2Reg = label = "";
        if (format.contains("@t")) {
            tReg = (quater.target.realReg >= 0) ? getRegName(quater.target.realReg) : "$t8";
        }
        if (format.contains("@x1")) {
            if (quater.x1 instanceof VirtualReg)
                x1RegInst = loadVregToReg((VirtualReg) quater.x1, "$t8");
            else
                x1RegInst = String.valueOf(((InstNumber) quater.x1).number);
        }
        if (format.contains("@x2")) {
            if (quater.x2 instanceof VirtualReg)
                x2RegInst = loadVregToReg((VirtualReg) quater.x2, "$t9");
            else
                x2RegInst = String.valueOf(((InstNumber) quater.x2).number);
        }
        if (format.contains("@rx1")) {
            if (isZero(quater.x1))
                x1Reg = "$zero";
            else if (quater.x1 instanceof VirtualReg)
                x1Reg = loadVregToReg((VirtualReg) quater.x1, "$t8");
            else {
                addMips("li $t8, %d", ((InstNumber) quater.x1).number);
                x1Reg = "$t8";
            }
        }
        if (format.contains("@rx2")) {
            if (isZero(quater.x2))
                x2Reg = "$zero";
            else if (quater.x2 instanceof VirtualReg)
                x2Reg = loadVregToReg((VirtualReg) quater.x2, "$t9");
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
        addMips(".data");
        Wrap<Integer> stringIdx = new Wrap<>(1);
        inter.forEach(p -> {
            switch (p.get().op) {
                case PRINT_STR:
                    // 将 print_str 的 label 改为 str_%d
                    addMips("str_%d: .asciiz \"%s\"", stringIdx.get(), p.get().label);
                    p.get().label = new Label("str_" + stringIdx.get());
                    stringIdx.set(stringIdx.get() + 1);
                    break;
                case GLOBAL_ALLOC: {
                    int size = ((InstNumber) p.get().x1).number;
                    if (p.get().list == null || p.get().list.size() == 0) {
                        addMips("%s: .space %d", p.get().label, size * 4);
                    }
                    else {
                        assert p.get().list.size() == size;
                        StringBuilder sb = new StringBuilder();
                        sb.append(p.get().label).append(": .word ");
                        for (Operand o : p.get().list) {
                            int wordVal = 0; // 不能确定的值，word 段填写 0
                            if (o instanceof InstNumber) wordVal = ((InstNumber) o).number;
                            sb.append(wordVal).append(", ");
                        }
                        addMips(sb.substring(0, sb.length() - 2));
                    }
                    break;
                }
            }
        });

        addMips(".text");
        inter.forEach(p -> {
            OperatorType op = p.get().op;
//            addMips("# %s (t=%s, x1=%s, x2=%s, label=%s)", op.name(), p.get().target, p.get().x1, p.get().x2, p.get().label);
            switch (op) {
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
                        assert p.get().list != null;
                        VirtualReg paramDef = allocInfo.getFuncParam(funcName, i); // 形参（保留在栈上还是寄存器中）
                        Operand paramCall = p.get().list.get(i); // 实参（是 vreg 还是立即数）

                        // 建立 实参 -> 形参 的传递
                        if (paramDef.realReg >= 0) {
                            String paramDefReg = getRegName(paramDef.realReg);
                            if (paramCall instanceof InstNumber) {
                                addMips("li %s, %d", paramDefReg, ((InstNumber) paramCall).number);
                            }
                            else if (paramCall instanceof VirtualReg) {
                                String paramCallReg = loadVregToReg((VirtualReg) paramCall, "$t8");
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
                                String paramCallReg = loadVregToReg((VirtualReg) paramCall, "$t8");
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
                case ALLOC: {
                    int arrayOffset = allocInfo.getVregOffset(p.get().target) + 4;
                    addRegMips(String.format("add @t, $sp, %d", arrayOffset), p.get());
                    for (int i = 0; p.get().list != null && i < p.get().list.size(); i++) {
                        Operand o = p.get().list.get(i);
                        if (o instanceof InstNumber) {
                            addMips("li $t8, %d", ((InstNumber) o).number);
                            addMips("sw $t8, %d($sp)", arrayOffset + i * 4);
                        }
                        else {
                            String initValReg = loadVregToReg((VirtualReg) o, "$t8");
                            addMips("sw %s, %d($sp)", initValReg, arrayOffset + i * 4);
                        }
                    }
                    break;
                }
                case GLOBAL_ALLOC: {
                    // 设置初值未确定的变量的值
                    for (int i = 0; p.get().list != null && i < p.get().list.size(); i++) {
                        Operand o = p.get().list.get(i);
                        if (o instanceof VirtualReg) {
                            String initValReg = loadVregToReg((VirtualReg) o, "$t8");
                            addMips("sw %s, %s + %d", initValReg, p.get().label, i * 4);
                        }
                    }
                    break;
                }
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
                case GET_GLOBAL_ARRAY:
                    // @t = @label[@x2]
                    if (p.get().x2 instanceof InstNumber) {
                        addRegMips(String.format("lw @t, @label + %d", ((InstNumber) p.get().x2).number * 4), p.get());
                    }
                    else {
                        addRegMips("sll $t9, @rx2, 2", p.get());
                        addRegMips("lw @t, @label($t9)", p.get());
                    }
                    break;
                case SET_ARRAY: {
                    // @t[@x1] = @x2，@t 在这里不会被改变，因此不要使用含有 @t 的 addRegMips
                    // 最终形式为 sw valueReg, offsetReg(baseReg)
                    String baseReg = loadVregToReg(p.get().target, "$t8");
                    String offsetReg;
                    if (p.get().x1 instanceof InstNumber) {
                        offsetReg = String.valueOf(((InstNumber) p.get().x1).number * 4);
                    }
                    else {
                        String indexReg = loadVregToReg((VirtualReg) p.get().x1, "$t9");
                        addMips("sll %s, %s, 2", indexReg, indexReg);
                        addMips("add %s, %s, %s", baseReg, baseReg, indexReg);
                        offsetReg = "0";
                    }
                    addRegMips(String.format("sw @rx2, %s(%s)", offsetReg, baseReg), p.get());
                    break;
                }
                case SET_GLOBAL_ARRAY:
                    // @label[@x1] = @x2
                    if (p.get().x1 instanceof InstNumber) {
                        addRegMips(String.format("sw @rx2, @label + %d", ((InstNumber) p.get().x1).number * 4), p.get());
                    }
                    else {
                        String indexReg = loadVregToReg((VirtualReg) p.get().x1, "$t8");
                        addMips("sll %s, %s, 2", indexReg, indexReg);
                        addRegMips(String.format("sw @rx2, @label(%s)", indexReg), p.get());
                    }
                    break;
                case ADD_ADDR:
                    // @&t = @&x1 + @x2
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
                case ADD_GLOBAL_ADDR:
                    // @&t = @label + @x2
                    addMips("la $t8, %s", p.get().label);
                    if (p.get().x2 instanceof InstNumber) {
                        addRegMips(String.format("add @t, $t8, %d", ((InstNumber) p.get().x2).number * 4), p.get());
                    }
                    else {
                        addRegMips("sll $t9, @rx2, 2", p.get());
                        addRegMips("add @t, $t8, $t9", p.get());
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
                        addRegMips("slt @t, @rx1, @rx2", p.get());
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
                    addMips("la $a0, %s", p.get().label);
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

package mips;

import intercode.Label;
import intercode.Operand;
import intercode.Operand.InstNumber;
import intercode.Operand.VirtualReg;
import intercode.Quaternion;
import mips.mipsoptimizer.PeepHole;
import mips.mipsoptimizer.WeakenDiv;
import mips.mipsoptimizer.WeakenMult;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.*;

public class MipsCoder {
    private final List<Quaternion> inter;
    private final List<Mips> mipsList = new ArrayList<>();
    private Map<String, FunctionInfo> funcInfoMap;
    private static final int A0 = 4;

    public MipsCoder(List<Quaternion> inter) {
        this.inter = inter;
    }

    public void generateMips() {
        this.funcInfoMap = Allocator.alloc(inter);
        funcInfoMap.values().forEach(System.out::println);
        generate();
        PeepHole.run(mipsList);
        WeakenDiv.run(mipsList);
        WeakenMult.run(mipsList);
    }

    public void output(String filename) throws IOException {
        StringBuilder result = new StringBuilder();
        mipsList.forEach(mips -> result.append(mips.code).append('\n'));
        Files.write(Paths.get(filename), result.toString().getBytes(StandardCharsets.UTF_8));
    }

    private static final String[] regNames = {"zero", "at", "v0", "v1", "a0", "a1", "a2", "a3", "t0", "t1", "t2", "t3", "t4", "t5", "t6", "t7", "s0", "s1", "s2", "s3", "s4", "s5", "s6", "s7", "t8", "t9", "k0", "k1", "gp", "sp", "fp", "ra"};

    public static String getRegName(int reg) {
        return "$" + regNames[reg];
    }

    private void addMips(String format, Object... args) {
        assert !format.contains("@");
        mipsList.add(new Mips(String.format(format, args)));
    }

    // 获取 vreg 对应的 reg；如果 vreg 存放在栈上，就从栈上加载到 defaultReg 中
    private String loadVregToReg(VirtualReg vreg, String defaultReg) {
        if (vreg.realReg >= 0) return getRegName(vreg.realReg);
        if (vreg.isGlobal)
            addMips("lw %s, %d($gp)", defaultReg, vreg.stackOffset);
        else
            addMips("lw %s, %d($sp)", defaultReg, vreg.stackOffset);
        return defaultReg;
    }

    // 涉及到虚寄存器的语句，对未分配的虚寄存器进行 lw/sw，对已分配的虚寄存器进行直接翻译
    // format 中，@t 表示 target，@x1 表示 x1，@x2 表示 x2，@label 表示 label
    // @rx1 表示必须是寄存器的 x1，若 x1 是立即数，会额外添加 li 指令；@rx2 同理
    private void addRegMips(String format, Quaternion quater) {
        String tReg, x1RegInst, x2RegInst, x1Reg, x2Reg, label;
        tReg = x1RegInst = x2RegInst = x1Reg = x2Reg = label = "";
        if (format.contains("@t")) {
            tReg = (quater.target.realReg >= 0) ?
                    getRegName(quater.target.realReg) : "$t8";
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
                    addMips("sw %s, %d($gp)", tReg, quater.target.stackOffset);
                else
                    addMips("sw %s, %d($sp)", tReg, quater.target.stackOffset);
            }
        }
    }

    private void generate() {
        addMips(".data");
        int stringIdx = 1;
        for (Quaternion q : inter) {
            switch (q.op) {
                case PRINT_STR:
                    // 将 print_str 的 label 改为 str_%d
                    addMips("str_%d: .asciiz \"%s\"", stringIdx, q.label);
                    q.label = new Label("str_" + stringIdx);
                    stringIdx++;
                    break;
                case GLOBAL_ALLOC: {
                    int size = ((InstNumber) q.x1).number;
                    if (q.list == null || q.list.size() == 0) {
                        addMips("%s: .space %d", q.label, size * 4);
                    }
                    else {
                        assert q.list.size() == size;
                        StringBuilder sb = new StringBuilder();
                        sb.append(q.label).append(": .word ");
                        for (Operand o : q.list) {
                            int wordVal = 0; // 不能确定的值，word 段填写 0
                            if (o instanceof InstNumber) wordVal = ((InstNumber) o).number;
                            sb.append(wordVal).append(", ");
                        }
                        addMips(sb.substring(0, sb.length() - 2));
                    }
                    break;
                }
            }
        }

        addMips(".text");
        for (Quaternion q : inter) {
            switch (q.op) {
                case FUNC: {
                    addMips("jr $ra");
                    addMips("func_%s:", q.label.name);
                    for (int i = 0; i < q.list.size(); i++) {
                        VirtualReg param = (VirtualReg) q.list.get(i);
                        // 寄存器传参
                        if (i < 4) {
                            // 目标参数在寄存器上
                            if (param.realReg >= 0) {
                                addMips("move %s, %s", getRegName(param.realReg), getRegName(A0 + i));
                            }
                            // 目标参数在栈上
                            else {
                                assert param.stackOffset >= 0;
                                addMips("sw %s, %d($sp)", getRegName(A0 + i), param.stackOffset);
                            }
                        }
                        // 栈传参
                        else {
                            int stackOffset = (i - 4) * 4;
                            if (param.realReg >= 0) {
                                addMips("lw %s, %d($sp)", getRegName(param.realReg), stackOffset);
                            }
                            else { // 栈传参且目标参数也在栈上，概率很小，可以优化但是不做了
                                assert param.stackOffset >= 0;
                                addMips("lw $t8, %d($sp)", stackOffset);
                                addMips("sw $t8, %d($sp)", param.stackOffset);
                            }
                        }
                    }
                    break;
                }
                case RETURN:
                    addMips("jr $ra");
                    break;
                case SET_RETURN:
                    if (q.x1 instanceof VirtualReg)
                        addRegMips("move $v0, @rx1", q);
                    else
                        addRegMips("li $v0, @x1", q);
                    break;
                case GET_RETURN:
                    addRegMips("move @t, $v0", q);
                    break;
                // 对于函数的调用者：
                // 1. 存放目标函数需要的参数
                // 2. 存放当前 $ra 到 0($sp) 位置
                // 3. 对当前活跃的寄存器保存现场，位置在 $sp 的上方，从 -4($sp) 开始
                // 4. 按照目标函数的调用栈大小，向小地址移动 $sp
                // 5. jal
                // 6. 按照目标函数的调用栈大小，恢复 $sp
                // 7. 恢复 $ra 和保存的寄存器
                case CALL: {
                    String callingFuncName = q.label.name;
                    // 总栈帧大小 frameSize + saveRegSize
                    int frameSize = funcInfoMap.get(callingFuncName).frameSize;
                    // 计算需要保存的活跃的寄存器
                    // 如果被调用函数是 pure 的，就只保存活跃寄存器与 callee.regUseSet 的交集
                    assert q.activeRegSet != null;
                    Set<Integer> regToSave;
                    if (!funcInfoMap.get(callingFuncName).isPureFunc)
                        regToSave = q.activeRegSet;
                    else {
                        regToSave = new HashSet<>(q.activeRegSet);
                        regToSave.retainAll(funcInfoMap.get(callingFuncName).regUseSet);
                    }
                    int saveRegSize = regToSave.size() * 4;

                    // 传参，前 4 个参数放在 $a0 ~ $a3 中，之后的参数放在栈上
                    for (int i = 0; i < q.list.size(); i++) {
                        Operand param = q.list.get(i); // 实参（是 vreg 还是立即数）
                        if (i < 4) {
                            int targetRegId = A0 + i;
                            if (param instanceof InstNumber)
                                addMips("li %s, %d", getRegName(targetRegId), ((InstNumber) param).number);
                            else {
                                String paramReg = loadVregToReg((VirtualReg) param, "$t8");
                                addMips("move %s, %s", getRegName(targetRegId), paramReg);
                            }
                        }
                        else {
                            int targetOffset = (i - 4) * 4 - frameSize - saveRegSize;
                            if (param instanceof InstNumber) {
                                addMips("li $t8, %d", ((InstNumber) param).number);
                                addMips("sw $t8, %d($sp)", targetOffset);
                            }
                            else {
                                String paramReg = loadVregToReg((VirtualReg) param, "$t8");
                                addMips("sw %s, %d($sp)", paramReg, targetOffset);
                            }
                        }
                    }
                    addMips("sw $ra, 0($sp)");

                    // 保存需要保存的寄存器
                    if (regToSave.size() > 0) {
                        int offset = -4;
                        for (Integer reg : regToSave) {
                            addMips("sw %s, %d($sp)", getRegName(reg), offset);
                            offset -= 4;
                        }
                    }
                    addMips("add $sp, $sp, -%d", frameSize + saveRegSize);
                    addMips("jal func_%s", callingFuncName);
                    addMips("add $sp, $sp, %d", frameSize + saveRegSize);
                    if (regToSave.size() > 0) {
                        int offset = -4;
                        for (Integer reg : regToSave) {
                            addMips("lw %s, %d($sp)", getRegName(reg), offset);
                            offset -= 4;
                        }
                    }
                    addMips("lw $ra, 0($sp)");
                    break;
                }
                case ENTER_MAIN:
                    addMips("add $sp, $sp, -%d", funcInfoMap.get("main").frameSize);
                    addMips("jal func_main");
                    addMips("li $v0, 10");
                    addMips("syscall");
                    break;
                case LABEL:
                    addMips("%s:", q.label.name);
                    break;
                case SET:
                    if (q.x1 instanceof VirtualReg)
                        addRegMips("move @t, @rx1", q);
                    else
                        addRegMips("li @t, @x1", q);
                    break;
                case ALLOC: {
                    int arrayOffset = q.target.stackOffset + 4;
                    if (q.list != null) {
                        for (int i = 0; i < q.list.size(); i++) {
                            Operand o = q.list.get(i);
                            if (isZero(o)) {
                                addMips("sw $zero, %d($sp)", arrayOffset + i * 4);
                            }
                            else if (o instanceof InstNumber) {
                                addMips("li $t8, %d", ((InstNumber) o).number);
                                addMips("sw $t8, %d($sp)", arrayOffset + i * 4);
                            }
                            else {
                                String initValReg = loadVregToReg((VirtualReg) o, "$t8");
                                addMips("sw %s, %d($sp)", initValReg, arrayOffset + i * 4);
                            }
                        }
                    }
                    addRegMips(String.format("add @t, $sp, %d", arrayOffset), q);
                    break;
                }
                case GLOBAL_ALLOC: {
                    // 设置初值未确定的变量的值
                    for (int i = 0; q.list != null && i < q.list.size(); i++) {
                        Operand o = q.list.get(i);
                        if (o instanceof VirtualReg) {
                            String initValReg = loadVregToReg((VirtualReg) o, "$t8");
                            addMips("sw %s, %s + %d", initValReg, q.label, i * 4);
                        }
                    }
                    break;
                }
                case GET_ARRAY: {
                    // @t = @x1[@x2]
                    if (q.x2 instanceof InstNumber) {
                        addRegMips(String.format("lw @t, %d(@rx1)", ((InstNumber) q.x2).number * 4), q);
                    }
                    else {
                        addRegMips("sll $t9, @rx2, 2", q);
                        addRegMips("add $t9, $t9, @x1", q);
                        addRegMips("lw @t, 0($t9)", q);
                    }
                    break;
                }
                case GET_GLOBAL_ARRAY:
                    // @t = @label[@x2]
                    if (q.x2 instanceof InstNumber) {
                        addRegMips(String.format("lw @t, @label + %d", ((InstNumber) q.x2).number * 4), q);
                    }
                    else {
                        addRegMips("sll $t9, @rx2, 2", q);
                        addRegMips("lw @t, @label($t9)", q);
                    }
                    break;
                case SET_ARRAY: {
                    // @t[@x1] = @x2，@t 在这里不会被改变，因此不要使用含有 @t 的 addRegMips
                    // 最终形式为 sw valueReg, offsetReg(baseReg)
                    String baseReg = loadVregToReg(q.target, "$t8");
                    if (q.x1 instanceof InstNumber) {
                        int offset = ((InstNumber) q.x1).number * 4;
                        addRegMips(String.format("sw @rx2, %d(%s)", offset, baseReg), q);
                    }
                    else {
                        String indexReg = loadVregToReg((VirtualReg) q.x1, "$t9");
                        addMips("sll $t9, %s, 2", indexReg);
                        addMips("add $t8, %s, $t9", baseReg);
                        addRegMips("sw @rx2, 0($t8)", q);
                    }
                    break;
                }
                case SET_GLOBAL_ARRAY:
                    // @label[@x1] = @x2
                    if (q.x1 instanceof InstNumber) {
                        addRegMips(String.format("sw @rx2, @label + %d", ((InstNumber) q.x1).number * 4), q);
                    }
                    else {
                        String indexReg = loadVregToReg((VirtualReg) q.x1, "$t8");
                        addMips("sll $t8, %s, 2", indexReg, indexReg);
                        addRegMips("sw @rx2, @label($t8)", q);
                    }
                    break;
                case ADD_ADDR:
                    // @&t = @&x1 + @x2
                    assert q.target.isAddr;
                    assert q.x1 instanceof VirtualReg && ((VirtualReg) q.x1).isAddr;
                    if (q.x2 instanceof InstNumber) {
                        addRegMips(String.format("add @t, @rx1, %d", ((InstNumber) q.x2).number * 4), q);
                    }
                    else if (q.x2 instanceof VirtualReg) {
                        addRegMips("sll $t9, @rx2, 2", q);
                        addRegMips("add @t, @rx1, $t9", q);
                    }
                    break;
                case ADD_GLOBAL_ADDR:
                    // @&t = @label + @x2
                    addMips("la $t8, %s", q.label);
                    if (q.x2 instanceof InstNumber) {
                        addRegMips(String.format("add @t, $t8, %d", ((InstNumber) q.x2).number * 4), q);
                    }
                    else {
                        addRegMips("sll $t9, @rx2, 2", q);
                        addRegMips("add @t, $t8, $t9", q);
                    }
                    break;
                case ADD:
                    addRegMips("add @t, @rx1, @x2", q);
                    break;
                case SUB:
                    if (q.x2 instanceof VirtualReg)
                        addRegMips("sub @t, @rx1, @rx2", q);
                    else
                        addRegMips(String.format("add @t, @rx1, %d", -((InstNumber) q.x2).number), q);
                    break;
                case MULT:
                    addRegMips("mul @t, @rx1, @x2", q);
                    break;
                case DIV:
                    addRegMips("div @rx1, @rx2", q);
                    addRegMips("mflo @t", q);
                    break;
                case MOD:
                    addRegMips("div @rx1, @rx2", q);
                    addRegMips("mfhi @t", q);
                    break;
                case NEG:
                    if (q.x1 instanceof InstNumber)
                        addRegMips(String.format("li @t, %d", -((InstNumber) q.x1).number), q);
                    else
                        addRegMips("sub @t, $zero, @rx1", q);
                    break;
                case NOT:
                    addRegMips("seq @t, @x1, $zero", q);
                    break;
                case EQ:
                    addRegMips("seq @t, @rx1, @x2", q);
                    break;
                case NOT_EQ:
                    addRegMips("sne @t, @rx1, @x2", q);
                    break;
                case LESS:
                    if (q.x2 instanceof InstNumber) {
                        int x = ((InstNumber) q.x2).number;
                        if (-32768 <= x && x <= 32767)
                            addRegMips("slti @t, @rx1, @x2", q);
                        else
                            addRegMips("slt @t, @rx1, @rx2", q);
                    }
                    else
                        addRegMips("slt @t, @rx1, @rx2", q);
                    break;
                case LESS_EQ:
                    addRegMips("sle @t, @rx1, @x2", q);
                    break;
                case GREATER:
                    addRegMips("sgt @t, @rx1, @x2", q);
                    break;
                case GREATER_EQ:
                    addRegMips("sge @t, @rx1, @x2", q);
                    break;
                case GOTO:
                    addRegMips("j @label", q);
                    break;
                case IF:
                    addRegMips("bne @rx1, $zero, @label", q);
                    break;
                case IF_NOT:
                    addRegMips("beq @rx1, $zero, @label", q);
                    break;
                case IF_EQ:
                    if (isZero(q.x2))
                        addRegMips("beq @rx1, $zero, @label", q);
                    else
                        addRegMips("beq @rx1, @x2, @label", q);
                    break;
                case IF_NOT_EQ:
                    if (isZero(q.x2))
                        addRegMips("bne @rx1, $zero, @label", q);
                    else
                        addRegMips("bne @rx1, @x2, @label", q);
                    break;
                case IF_LESS:
                    if (isZero(q.x2))
                        addRegMips("bltz @rx1, @label", q);
                    else
                        addRegMips("blt @rx1, @x2, @label", q);
                    break;
                case IF_LESS_EQ:
                    if (isZero(q.x2))
                        addRegMips("blez @rx1, @label", q);
                    else
                        addRegMips("ble @rx1, @x2, @label", q);
                    break;
                case IF_GREATER:
                    if (isZero(q.x2))
                        addRegMips("bgtz @rx1, @label", q);
                    else
                        addRegMips("bgt @rx1, @x2, @label", q);
                    break;
                case IF_GREATER_EQ:
                    if (isZero(q.x2))
                        addRegMips("bgez @rx1, @label", q);
                    else
                        addRegMips("bge @rx1, @x2, @label", q);
                    break;
                case GETINT:
                    addMips("li $v0, 5");
                    addMips("syscall");
                    addRegMips("move @t, $v0", q);
                    break;
                case PRINT_STR:
                    addMips("la $a0, %s", q.label);
                    addMips("li $v0, 4");
                    addMips("syscall");
                    break;
                case PRINT_CHAR:
                    if (q.x1 instanceof VirtualReg)
                        addRegMips("move $a0, @rx1", q);
                    else
                        addRegMips("li $a0, @x1", q);
                    addMips("li $v0, 11");
                    addMips("syscall");
                    break;
                case PRINT_INT:
                    if (q.x1 instanceof VirtualReg)
                        addRegMips("move $a0, @rx1", q);
                    else
                        addRegMips("li $a0, @x1", q);
                    addMips("li $v0, 1");
                    addMips("syscall");
                    break;
            }
        }
    }

    private boolean isZero(Operand x) {
        if (x instanceof VirtualReg) return false;
        else return ((InstNumber) x).number == 0;
    }
}

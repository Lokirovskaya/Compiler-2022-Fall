package mips;

import intercode.InterCode;
import intercode.Label;
import intercode.Operand;
import intercode.Operand.InstNumber;
import intercode.Operand.VirtualReg;
import intercode.Quaternion;
import util.NodeList;
import util.Wrap;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Map;

public class MipsCoder {
    private final InterCode inter;
    private final NodeList<Mips> mipsList = new NodeList<>();
    private Map<String, FunctionInfo> funcInfoMap;
    private static final int A0 = 4;

    public MipsCoder(InterCode inter) {
        this.inter = inter;
    }

    public void generateMips() {
        this.funcInfoMap = Allocator.alloc(inter);
        funcInfoMap.values().forEach(System.out::println);
        generate();
        MipsOptimizer.optimize(mipsList);
    }

    public void output(String filename) throws IOException {
        StringBuilder result = new StringBuilder();
        mipsList.forEachItem(quater -> result.append(quater.code).append('\n'));
        Files.write(Paths.get(filename), result.toString().getBytes(StandardCharsets.UTF_8));
    }

    private static final String[] regNames = {"zero", "at", "v0", "v1", "a0", "a1", "a2", "a3", "t0", "t1", "t2", "t3", "t4", "t5", "t6", "t7", "s0", "s1", "s2", "s3", "s4", "s5", "s6", "s7", "t8", "t9", "k0", "k1", "gp", "sp", "fp", "ra"};

    public static String getRegName(int reg) {
        return "$" + regNames[reg];
    }

    private void addMips(String format, Object... args) {
        assert !format.contains("@");
        mipsList.addLast(new Mips(String.format(format, args)));
    }

    // 获取 vreg 对应的 reg；如果 vreg 存放在栈上，就从栈上加载到 defaultReg 中
    private String loadVregToReg(VirtualReg vreg, int lineNumber, String defaultReg) {
        if (vreg.getRealReg(lineNumber) >= 0) return getRegName(vreg.getRealReg(lineNumber));
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
        int lineNumber = quater.id;
        if (format.contains("@t")) {
            tReg = (quater.target.getRealReg(lineNumber) >= 0) ?
                    getRegName(quater.target.getRealReg(lineNumber)) : "$t8";
        }
        if (format.contains("@x1")) {
            if (quater.x1 instanceof VirtualReg)
                x1RegInst = loadVregToReg((VirtualReg) quater.x1, lineNumber, "$t8");
            else
                x1RegInst = String.valueOf(((InstNumber) quater.x1).number);
        }
        if (format.contains("@x2")) {
            if (quater.x2 instanceof VirtualReg)
                x2RegInst = loadVregToReg((VirtualReg) quater.x2, lineNumber, "$t9");
            else
                x2RegInst = String.valueOf(((InstNumber) quater.x2).number);
        }
        if (format.contains("@rx1")) {
            if (isZero(quater.x1))
                x1Reg = "$zero";
            else if (quater.x1 instanceof VirtualReg)
                x1Reg = loadVregToReg((VirtualReg) quater.x1, lineNumber, "$t8");
            else {
                addMips("li $t8, %d", ((InstNumber) quater.x1).number);
                x1Reg = "$t8";
            }
        }
        if (format.contains("@rx2")) {
            if (isZero(quater.x2))
                x2Reg = "$zero";
            else if (quater.x2 instanceof VirtualReg)
                x2Reg = loadVregToReg((VirtualReg) quater.x2, lineNumber, "$t9");
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
            if (quater.target.getRealReg(lineNumber) < 0) {
                if (quater.target.isGlobal)
                    addMips("sw %s, %d($gp)", tReg, quater.target.stackOffset);
                else
                    addMips("sw %s, %d($sp)", tReg, quater.target.stackOffset);
            }
        }
    }

    private void generate() {
        addMips(".data");
        Wrap<Integer> stringIdx = new Wrap<>(1);
        inter.forEachItem(quater -> {
            switch (quater.op) {
                case PRINT_STR:
                    // 将 print_str 的 label 改为 str_%d
                    addMips("str_%d: .asciiz \"%s\"", stringIdx.get(), quater.label);
                    quater.label = new Label("str_" + stringIdx.get());
                    stringIdx.set(stringIdx.get() + 1);
                    break;
                case GLOBAL_ALLOC: {
                    int size = ((InstNumber) quater.x1).number;
                    if (quater.list == null || quater.list.size() == 0) {
                        addMips("%s: .space %d", quater.label, size * 4);
                    }
                    else {
                        assert quater.list.size() == size;
                        StringBuilder sb = new StringBuilder();
                        sb.append(quater.label).append(": .word ");
                        for (Operand o : quater.list) {
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
        inter.forEachItem(quater -> {
//            addMips("# %s (t=%s, x1=%s, x2=%s, label=%s)", op.name(), quater.target, quater.x1, quater.x2, quater.label);
            switch (quater.op) {
                case FUNC:
                    addMips("jr $ra");
                    addMips("func_%s:", quater.label.name);
                    for (int i = 0; i < quater.list.size(); i++) {
                        VirtualReg param = (VirtualReg) quater.list.get(i);
                        // 寄存器传参
                        if (i < 4) {
                            // 目标参数在寄存器上
                            if (param.getRealReg(quater.id) >= 0) {
                                addMips("move %s, %s",
                                        getRegName(param.getRealReg(quater.id)),
                                        getRegName(A0 + i));
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
                            if (param.getRealReg(quater.id) >= 0) {
                                addMips("lw %s, %d($sp)", getRegName(A0 + i), stackOffset);
                            }
                            else { // 概率很小，可以优化但是不做了
                                assert param.stackOffset >= 0;
                                addMips("lw $t8, %d($sp)", stackOffset);
                                addMips("sw $t8, %d($sp)", param.stackOffset);
                            }
                        }
                    }
                    break;
                case RETURN:
                    addMips("jr $ra");
                    break;
                case SET_RETURN:
                    if (quater.x1 instanceof VirtualReg)
                        addRegMips("move $v0, @rx1", quater);
                    else
                        addRegMips("li $v0, @x1", quater);
                    break;
                case GET_RETURN:
                    addRegMips("move @t, $v0", quater);
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
                    String funcName = quater.label.name;
                    // 总栈帧大小 frameSize + saveRegSize
                    int frameSize = funcInfoMap.get(funcName).frameSize;
                    int saveRegSize = quater.activeRegList == null ? 0 : quater.activeRegList.size() * 4;

                    // 前 4 个参数放在 $a0 ~ $a3 中，之后的参数放在栈上
                    for (int i = 0; i < quater.list.size(); i++) {
                        Operand param = quater.list.get(i); // 实参（是 vreg 还是立即数）
                        if (i < 4) {
                            int targetRegId = A0 + i;
                            if (param instanceof InstNumber)
                                addMips("li %s, %d", getRegName(targetRegId), ((InstNumber) param).number);
                            else {
                                String paramReg = loadVregToReg((VirtualReg) param, quater.id, "$t8");
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
                                String paramReg = loadVregToReg((VirtualReg) param, quater.id, "$t8");
                                addMips("sw %s, %d($sp)", paramReg, targetOffset);
                            }
                        }
                    }
                    addMips("sw $ra, 0($sp)");
                    if (quater.activeRegList != null) {
                        int offset = -4;
                        for (Integer reg : quater.activeRegList) {
                            addMips("sw %s, %d($sp)", getRegName(reg), offset);
                            offset -= 4;
                        }
                    }
                    addMips("add $sp, $sp, -%d", frameSize + saveRegSize);
                    addMips("jal func_%s", funcName);
                    addMips("add $sp, $sp, %d", frameSize + saveRegSize);
                    if (quater.activeRegList != null) {
                        int offset = -4;
                        for (Integer reg : quater.activeRegList) {
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
                    addMips("%s:", quater.label.name);
                    break;
                case SET:
                    if (quater.x1 instanceof VirtualReg)
                        addRegMips("move @t, @rx1", quater);
                    else
                        addRegMips("li @t, @x1", quater);
                    break;
                case ALLOC: {
                    int arrayOffset = quater.target.stackOffset + 4;
                    addRegMips(String.format("add @t, $sp, %d", arrayOffset), quater);
                    for (int i = 0; quater.list != null && i < quater.list.size(); i++) {
                        Operand o = quater.list.get(i);
                        if (isZero(o)) {
                            addMips("sw $zero, %d($sp)", arrayOffset + i * 4);
                        }
                        else if (o instanceof InstNumber) {
                            addMips("li $t8, %d", ((InstNumber) o).number);
                            addMips("sw $t8, %d($sp)", arrayOffset + i * 4);
                        }
                        else {
                            String initValReg = loadVregToReg((VirtualReg) o, quater.id, "$t8");
                            addMips("sw %s, %d($sp)", initValReg, arrayOffset + i * 4);
                        }
                    }
                    break;
                }
                case GLOBAL_ALLOC: {
                    // 设置初值未确定的变量的值
                    for (int i = 0; quater.list != null && i < quater.list.size(); i++) {
                        Operand o = quater.list.get(i);
                        if (o instanceof VirtualReg) {
                            String initValReg = loadVregToReg((VirtualReg) o, quater.id, "$t8");
                            addMips("sw %s, %s + %d", initValReg, quater.label, i * 4);
                        }
                    }
                    break;
                }
                case GET_ARRAY: {
                    // @t = @x1[@x2]
                    if (quater.x2 instanceof InstNumber) {
                        addRegMips(String.format("lw @t, %d(@rx1)", ((InstNumber) quater.x2).number * 4), quater);
                    }
                    else {
                        addRegMips("sll $t9, @rx2, 2", quater);
                        addRegMips("add $t9, $t9, @x1", quater);
                        addRegMips("lw @t, 0($t9)", quater);
                    }
                    break;
                }
                case GET_GLOBAL_ARRAY:
                    // @t = @label[@x2]
                    if (quater.x2 instanceof InstNumber) {
                        addRegMips(String.format("lw @t, @label + %d", ((InstNumber) quater.x2).number * 4), quater);
                    }
                    else {
                        addRegMips("sll $t9, @rx2, 2", quater);
                        addRegMips("lw @t, @label($t9)", quater);
                    }
                    break;
                case SET_ARRAY: {
                    // @t[@x1] = @x2，@t 在这里不会被改变，因此不要使用含有 @t 的 addRegMips
                    // 最终形式为 sw valueReg, offsetReg(baseReg)
                    String baseReg = loadVregToReg(quater.target, quater.id, "$t8");
                    String offsetReg;
                    if (quater.x1 instanceof InstNumber) {
                        offsetReg = String.valueOf(((InstNumber) quater.x1).number * 4);
                    }
                    else {
                        String indexReg = loadVregToReg((VirtualReg) quater.x1, quater.id, "$t9");
                        addMips("sll %s, %s, 2", indexReg, indexReg);
                        addMips("add %s, %s, %s", baseReg, baseReg, indexReg);
                        offsetReg = "0";
                    }
                    addRegMips(String.format("sw @rx2, %s(%s)", offsetReg, baseReg), quater);
                    break;
                }
                case SET_GLOBAL_ARRAY:
                    // @label[@x1] = @x2
                    if (quater.x1 instanceof InstNumber) {
                        addRegMips(String.format("sw @rx2, @label + %d", ((InstNumber) quater.x1).number * 4), quater);
                    }
                    else {
                        String indexReg = loadVregToReg((VirtualReg) quater.x1, quater.id, "$t8");
                        addMips("sll %s, %s, 2", indexReg, indexReg);
                        addRegMips(String.format("sw @rx2, @label(%s)", indexReg), quater);
                    }
                    break;
                case ADD_ADDR:
                    // @&t = @&x1 + @x2
                    assert quater.target.isAddr;
                    assert quater.x1 instanceof VirtualReg && ((VirtualReg) quater.x1).isAddr;
                    if (quater.x2 instanceof InstNumber) {
                        addRegMips(String.format("add @t, @rx1, %d", ((InstNumber) quater.x2).number * 4), quater);
                    }
                    else if (quater.x2 instanceof VirtualReg) {
                        addRegMips("sll $t9, @rx2, 2", quater);
                        addRegMips("add @t, @rx1, $t9", quater);
                    }
                    break;
                case ADD_GLOBAL_ADDR:
                    // @&t = @label + @x2
                    addMips("la $t8, %s", quater.label);
                    if (quater.x2 instanceof InstNumber) {
                        addRegMips(String.format("add @t, $t8, %d", ((InstNumber) quater.x2).number * 4), quater);
                    }
                    else {
                        addRegMips("sll $t9, @rx2, 2", quater);
                        addRegMips("add @t, $t8, $t9", quater);
                    }
                    break;
                case ADD:
                    addRegMips("add @t, @rx1, @x2", quater);
                    break;
                case SUB:
                    if (quater.x2 instanceof VirtualReg)
                        addRegMips("sub @t, @rx1, @x2", quater);
                    else
                        addRegMips(String.format("add @t, @rx1, %d", -((InstNumber) quater.x2).number), quater);
                    break;
                case MULT:
                    addRegMips("mul @t, @rx1, @x2", quater);
                    break;
                case DIV:
                    addRegMips("div @rx1, @rx2", quater);
                    addRegMips("mflo @t", quater);
                    break;
                case MOD:
                    addRegMips("div @rx1, @rx2", quater);
                    addRegMips("mfhi @t", quater);
                    break;
                case NEG:
                    addRegMips("sub @t, $zero, @x1", quater);
                    break;
                case NOT:
                    addRegMips("seq @t, @x1, $zero", quater);
                    break;
                case EQ:
                    addRegMips("seq @t, @rx1, @x2", quater);
                    break;
                case NOT_EQ:
                    addRegMips("sne @t, @rx1, @x2", quater);
                    break;
                case LESS:
                    if (quater.x2 instanceof InstNumber) {
                        int x = ((InstNumber) quater.x2).number;
                        if (-32768 <= x && x <= 32767)
                            addRegMips("slti @t, @rx1, @x2", quater);
                        else
                            addRegMips("slt @t, @rx1, @rx2", quater);
                    }
                    else
                        addRegMips("slt @t, @rx1, @rx2", quater);
                    break;
                case LESS_EQ:
                    addRegMips("sle @t, @rx1, @x2", quater);
                    break;
                case GREATER:
                    addRegMips("sgt @t, @rx1, @x2", quater);
                    break;
                case GREATER_EQ:
                    addRegMips("sge @t, @rx1, @x2", quater);
                    break;
                case GOTO:
                    addRegMips("j @label", quater);
                    break;
                case IF:
                    addRegMips("bne @rx1, $zero, @label", quater);
                    break;
                case IF_NOT:
                    addRegMips("beq @rx1, $zero, @label", quater);
                    break;
                case IF_EQ:
                    if (isZero(quater.x2))
                        addRegMips("beq @rx1, $zero, @label", quater);
                    else
                        addRegMips("beq @rx1, @x2, @label", quater);
                    break;
                case IF_NOT_EQ:
                    if (isZero(quater.x2))
                        addRegMips("bne @rx1, $zero, @label", quater);
                    else
                        addRegMips("bne @rx1, @x2, @label", quater);
                    break;
                case IF_LESS:
                    if (isZero(quater.x2))
                        addRegMips("bltz @rx1, @label", quater);
                    else
                        addRegMips("blt @rx1, @x2, @label", quater);
                    break;
                case IF_LESS_EQ:
                    if (isZero(quater.x2))
                        addRegMips("blez @rx1, @label", quater);
                    else
                        addRegMips("ble @rx1, @x2, @label", quater);
                    break;
                case IF_GREATER:
                    if (isZero(quater.x2))
                        addRegMips("bgtz @rx1, @label", quater);
                    else
                        addRegMips("bgt @rx1, @x2, @label", quater);
                    break;
                case IF_GREATER_EQ:
                    if (isZero(quater.x2))
                        addRegMips("bgez @rx1, @label", quater);
                    else
                        addRegMips("bge @rx1, @x2, @label", quater);
                    break;
                case GETINT:
                    addMips("li $v0, 5");
                    addMips("syscall");
                    addRegMips("move @t, $v0", quater);
                    break;
                case PRINT_STR:
                    if (quater.activeRegList != null && quater.activeRegList.contains(A0))
                        addMips("move $t9, $a0");
                    addMips("la $a0, %s", quater.label);
                    addMips("li $v0, 4");
                    addMips("syscall");
                    if (quater.activeRegList != null && quater.activeRegList.contains(A0))
                        addMips("move $a0, $t9");
                    break;
                case PRINT_CHAR:
                    if (quater.activeRegList != null && quater.activeRegList.contains(A0))
                        addMips("move $t9, $a0");
                    if (quater.x1 instanceof VirtualReg)
                        addRegMips("move $a0, @rx1", quater);
                    else
                        addRegMips("li $a0, @x1", quater);
                    addMips("li $v0, 11");
                    addMips("syscall");
                    if (quater.activeRegList != null && quater.activeRegList.contains(A0))
                        addMips("move $a0, $t9");
                    break;
                case PRINT_INT:
                    if (quater.activeRegList != null && quater.activeRegList.contains(A0))
                        addMips("move $t9, $a0");
                    if (quater.x1 instanceof VirtualReg)
                        addRegMips("move $a0, @rx1", quater);
                    else
                        addRegMips("li $a0, @x1", quater);
                    addMips("li $v0, 1");
                    addMips("syscall");
                    if (quater.activeRegList != null && quater.activeRegList.contains(A0))
                        addMips("move $a0, $t9");
                    break;
            }
        });
    }

    private boolean isZero(Operand x) {
        if (x instanceof VirtualReg) return false;
        else return ((InstNumber) x).number == 0;
    }
}

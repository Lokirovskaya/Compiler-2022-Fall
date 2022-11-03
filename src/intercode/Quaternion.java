package intercode;

import intercode.Operand.VirtualReg;

import java.util.List;

import static intercode.Quaternion.OperatorType.*;

public class Quaternion {
    public int id;
    static int idTop = 1;
    public OperatorType op;
    public VirtualReg target;
    public Operand x1, x2;
    public Label label;
    public List<Operand> list;

    // not public
    Quaternion(OperatorType op, VirtualReg target, Operand x1, Operand x2, Label label) {
        this.id = idTop++;
        this.op = op;
        this.target = target;
        this.x1 = x1;
        this.x2 = x2;
        this.label = label;
    }

    public enum OperatorType {
        ADD, SUB, MULT, DIV, MOD, NEG, NOT, EQ, NOT_EQ, LESS, LESS_EQ, GREATER, GREATER_EQ,
        IF, GOTO, FUNC, CALL, RETURN, EXIT, SET_RETURN, GET_RETURN,
        SET, GET_ARRAY, SET_ARRAY, ADD_ADDR, GET_GLOBAL_ARRAY, SET_GLOBAL_ARRAY, ADD_GLOBAL_ADDR,
        LABEL, PARAM, ALLOC, GLOBAL_ALLOC,
        GETINT, PRINT_STR, PRINT_INT, PRINT_CHAR,
        // 优化中会出现的操作码
        IF_NOT, IF_EQ, IF_NOT_EQ, IF_LESS, IF_LESS_EQ, IF_GREATER, IF_GREATER_EQ,
    }

    @Override
    public String toString() {
        String str;
        if (op == SET) str = String.format("%s = %s", target, x1);
        else if (op == ADD) str = String.format("%s = %s + %s", target, x1, x2);
        else if (op == SUB) str = String.format("%s = %s - %s", target, x1, x2);
        else if (op == MULT) str = String.format("%s = %s * %s", target, x1, x2);
        else if (op == DIV) str = String.format("%s = %s / %s", target, x1, x2);
        else if (op == MOD) str = String.format("%s = %s %% %s", target, x1, x2);
        else if (op == NEG) str = String.format("%s = -%s", target, x1);
        else if (op == GET_ARRAY) str = String.format("%s = %s[%s]", target, x1, x2);
        else if (op == SET_ARRAY) str = String.format("%s[%s] = %s", target, x1, x2);
        else if (op == ADD_ADDR) str = String.format("%s = &(%s[%s])", target, x1, x2);
        else if (op == GET_GLOBAL_ARRAY) str = String.format("%s = %s[%s]", target, label, x2);
        else if (op == SET_GLOBAL_ARRAY) str = String.format("%s[%s] = %s", label, x1, x2);
        else if (op == ADD_GLOBAL_ADDR) str = String.format("%s = &(%s[%s])", target, label, x2);
        else if (op == IF) str = String.format("if %s goto %s", x1, label);
        else if (op == IF_NOT) str = String.format("if_not %s goto %s", x1, label);
        else if (op == IF_EQ) str = String.format("if_cond %s == %s goto %s", x1, x2, label);
        else if (op == IF_NOT_EQ) str = String.format("if_cond %s != %s goto %s", x1, x2, label);
        else if (op == IF_LESS) str = String.format("if_cond %s < %s goto %s", x1, x2, label);
        else if (op == IF_LESS_EQ) str = String.format("if_cond %s <= %s goto %s", x1, x2, label);
        else if (op == IF_GREATER) str = String.format("if_cond %s > %s goto %s", x1, x2, label);
        else if (op == IF_GREATER_EQ) str = String.format("if_cond %s >= %s goto %s", x1, x2, label);
        else if (op == NOT) str = String.format("%s = !%s", target, x1);
        else if (op == EQ) str = String.format("%s = %s == %s", target, x1, x2);
        else if (op == NOT_EQ) str = String.format("%s = %s != %s", target, x1, x2);
        else if (op == LESS) str = String.format("%s = %s < %s", target, x1, x2);
        else if (op == LESS_EQ) str = String.format("%s = %s <= %s", target, x1, x2);
        else if (op == GREATER) str = String.format("%s = %s > %s", target, x1, x2);
        else if (op == GREATER_EQ) str = String.format("%s = %s >= %s", target, x1, x2);
        else if (op == GETINT) str = String.format("%s = getint", target);
        else if (op == PRINT_INT) str = String.format("print_int %s", x1);
        else if (op == PRINT_STR) str = String.format("print_str \"%s\"", label);
        else if (op == PRINT_CHAR) str = String.format("print_char %s", x1);
        else if (op == FUNC) str = String.format("func %s", label);
        else if (op == LABEL) str = String.format("%s:", label);
        else if (op == GOTO) str = String.format("goto %s", label);
        else if (op == RETURN) str = "return";
        else if (op == SET_RETURN) str = String.format("RET = %s", x1);
        else if (op == GET_RETURN) str = String.format("%s = RET", target);
        else if (op == EXIT) str = "exit";
        else if (op == CALL) str = String.format("call %s %s", label, operandListToString(list));
        else if (op == PARAM) str = String.format("param %s", target);
        else if (op == ALLOC) str = String.format("%s = alloc %s %s", target, x1, operandListToString(list));
        else if (op == GLOBAL_ALLOC) str = String.format("%s = global_alloc %s %s", label, x1, operandListToString(list));
        else return null;
        return id + "  " + str;
    }

    private static String operandListToString(List<Operand> list) {
        if (list == null) return "()";
        else return "(" + list.toString().substring(1, list.toString().length() - 1) + ")";
    }
}

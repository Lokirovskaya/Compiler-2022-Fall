package intercode;

import java.util.List;

import static intercode.Quaternion.OperatorType.*;

public class Quaternion {
    public OperatorType op;
    public Operand.VirtualReg target;
    public Operand x1, x2;
    public Label label;
    public List<Operand> list;

    public Quaternion(OperatorType op, Operand.VirtualReg target, Operand x1, Operand x2, Label label) {
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
        if (op == SET) return String.format("%s = %s", target, x1);
        else if (op == ADD) return String.format("%s = %s + %s", target, x1, x2);
        else if (op == SUB) return String.format("%s = %s - %s", target, x1, x2);
        else if (op == MULT) return String.format("%s = %s * %s", target, x1, x2);
        else if (op == DIV) return String.format("%s = %s / %s", target, x1, x2);
        else if (op == MOD) return String.format("%s = %s %% %s", target, x1, x2);
        else if (op == NEG) return String.format("%s = -%s", target, x1);
        else if (op == GET_ARRAY) return String.format("%s = %s[%s]", target, x1, x2);
        else if (op == SET_ARRAY) return String.format("%s[%s] = %s", target, x1, x2);
        else if (op == ADD_ADDR) return String.format("%s = &(%s[%s])", target, x1, x2);
        else if (op == GET_GLOBAL_ARRAY) return String.format("%s = %s[%s]", target, label, x2);
        else if (op == SET_GLOBAL_ARRAY) return String.format("%s[%s] = %s", label, x1, x2);
        else if (op == ADD_GLOBAL_ADDR) return String.format("%s = &(%s[%s])", target, label, x2);
        else if (op == IF) return String.format("if %s goto %s", x1, label);
        else if (op == IF_NOT) return String.format("if_not %s goto %s", x1, label);
        else if (op == IF_EQ) return String.format("if_cond %s == %s goto %s", x1, x2, label);
        else if (op == IF_NOT_EQ) return String.format("if_cond %s != %s goto %s", x1, x2, label);
        else if (op == IF_LESS) return String.format("if_cond %s < %s goto %s", x1, x2, label);
        else if (op == IF_LESS_EQ) return String.format("if_cond %s <= %s goto %s", x1, x2, label);
        else if (op == IF_GREATER) return String.format("if_cond %s > %s goto %s", x1, x2, label);
        else if (op == IF_GREATER_EQ) return String.format("if_cond %s >= %s goto %s", x1, x2, label);
        else if (op == NOT) return String.format("%s = !%s", target, x1);
        else if (op == EQ) return String.format("%s = %s == %s", target, x1, x2);
        else if (op == NOT_EQ) return String.format("%s = %s != %s", target, x1, x2);
        else if (op == LESS) return String.format("%s = %s < %s", target, x1, x2);
        else if (op == LESS_EQ) return String.format("%s = %s <= %s", target, x1, x2);
        else if (op == GREATER) return String.format("%s = %s > %s", target, x1, x2);
        else if (op == GREATER_EQ) return String.format("%s = %s >= %s", target, x1, x2);
        else if (op == GETINT) return String.format("%s = getint", target);
        else if (op == PRINT_INT) return String.format("print_int %s", x1);
        else if (op == PRINT_STR) return String.format("print_str \"%s\"", label);
        else if (op == PRINT_CHAR) return String.format("print_char %s", x1);
        else if (op == FUNC) return String.format("func %s", label);
        else if (op == LABEL) return String.format("%s:", label);
        else if (op == GOTO) return String.format("goto %s", label);
        else if (op == RETURN) return "return";
        else if (op == SET_RETURN) return String.format("RET = %s", x1);
        else if (op == GET_RETURN) return String.format("%s = RET", target);
        else if (op == EXIT) return "exit";
        else if (op == CALL) return String.format("call %s %s", label, operandListToString(list));
        else if (op == PARAM) return String.format("param %s", target);
        else if (op == ALLOC) return String.format("%s = alloc %s %s", target, x1, operandListToString(list));
        else if (op == GLOBAL_ALLOC) return String.format("%s = global_alloc %s %s", label, x1, operandListToString(list));
        else return null;
    }

    private static String operandListToString(List<Operand> list) {
        if (list == null) return "()";
        else return "(" + list.toString().substring(1, list.toString().length() - 1) + ")";
    }
}

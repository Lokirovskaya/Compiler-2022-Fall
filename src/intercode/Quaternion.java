package intercode;

public class Quaternion {
    public OperatorType op;
    public Operand.VirtualReg target;
    public Operand x1, x2;
    public Label label;

    public Quaternion(OperatorType op, Operand.VirtualReg target, Operand x1, Operand x2, Label label) {
        this.op = op;
        this.target = target;
        this.x1 = x1;
        this.x2 = x2;
        this.label = label;
    }

    public enum OperatorType {
        ADD, SUB, MULT, DIV, MOD, NEG, NOT, EQ, NOT_EQ, LESS, LESS_EQ, GREATER, GREATER_EQ,
        IF, GOTO, FUNC, CALL, END_CALL, RETURN, EXIT,
        SET, GET_ARRAY, SET_ARRAY, ADD_ADDR,
        LABEL, PUSH, PARAM, ALLOC, STR_DECLARE,
        GETINT, PRINT_STR, PRINT_INT, PRINT_CHAR,
        // 优化中会出现的操作码
        IF_NOT, IF_EQ, IF_NOT_EQ, IF_LESS, IF_LESS_EQ, IF_GREATER, IF_GREATER_EQ,
        SHIFT_LEFT, SHIFT_RIGHT
    }
}

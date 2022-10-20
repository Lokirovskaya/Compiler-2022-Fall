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
        ADD, SUB, MULT, DIV, MOD, NOT, EQ, NOT_EQ, LESS, LESS_EQ, GREATER, GREATER_EQ,
        IF, IF_NOT, GOTO, FUNC, END_FUNC, CALL, RETURN, EXIT,
        SET, GET_ARRAY, SET_ARRAY, LOAD_ADDR,
        LABEL, PUSH, PARAM, PARAM_ARRAY, ALLOC, ALLOC_STR,
        GETINT, PRINT_STR, PRINT_INT, PRINT_CHAR
    }
}

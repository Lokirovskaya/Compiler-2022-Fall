package intercode;

public class Quaternion {
    public OperatorType op;
    public Operand.VirtualReg target;
    public Operand x1, x2;
    public Operand.Label label;

    public Quaternion(OperatorType op, Operand.VirtualReg target, Operand x1, Operand x2, Operand.Label label) {
        this.op = op;
        this.target = target;
        this.x1 = x1;
        this.x2 = x2;
        this.label = label;
    }


    public enum OperatorType {
        ADD, SUB, MULT, DIV, MOD,
        SET, GET_ARRAY, SET_ARRAY, LOAD_ADDR, // x=y, x=y[], x[]=y, x=&y
        FUNC, LABEL, GOTO, RETURN, CALL, LOAD_RETURN, PUSH, PARAM, PARAM_ARRAY, ALLOC,
        GETINT, PRINT_STR, PRINT_INT, PRINT_CHAR,
        IF_EQ, IF_NEQ, IF_LESS, IF_LESS_EQ, IF_GREATER, IF_GREATER_EQ
    }
}

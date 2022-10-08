package symbol;

import intercode.Operand;

import java.util.ArrayList;
import java.util.List;

public abstract class Symbol {
    public String name;
    public int lineNumber;
    public Table selfTable;

    public static class Var extends Symbol {
        public boolean isConst;
        public int dimension; // 普通变量为 0
        // 中间代码生成时填
        public Operand.VirtualReg reg;
        public List<Operand.VirtualReg> capacity;
    }

    public static class Function extends Symbol {
        public boolean isVoid;
        public List<Symbol.Var> params = new ArrayList<>();
    }
}

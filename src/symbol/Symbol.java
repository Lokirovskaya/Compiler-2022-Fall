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
        // 数组每一维的容量，从高维到低维；若维度 > 1，最高维容量（capacity[0]）可以不存
        // 已在生成符号时初始化
        public Operand.VirtualReg[] capacity;
    }

    public static class Function extends Symbol {
        public boolean isVoid;
        public List<Symbol.Var> params = new ArrayList<>();
    }
}

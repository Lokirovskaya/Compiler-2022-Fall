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
        public Operand.VirtualReg reg;
        // 对于三维数组 int a[x][y][z]
        // volume[0] = y*z; volume[1] = z; volume[2] = 1; x 是无用的，不需要计算
        // 储存的是上述运算的运算结果 reg，事实上 volume[-1] == null
        public Operand.VirtualReg[] volume;

        public boolean isArray() {
            return dimension > 0;
        }
    }

    public static class Function extends Symbol {
        public boolean isVoid;
        public List<Symbol.Var> params = new ArrayList<>();
    }
}

package symbol;

import intercode.Label;
import intercode.Operand;

import java.util.ArrayList;
import java.util.List;

public abstract class Symbol {
    public String name;
    public int lineNumber;
    public Table selfTable;

    public boolean isGlobal() {
        return selfTable.id == 0;
    }

    public static class Var extends Symbol {
        public boolean isConst;
        public int dimension; // 普通变量为 0
        public Operand.VirtualReg reg;
        public Operand.InstNumber sizeOfDim1, sizeOfDim2;
        public Operand.InstNumber constVal;
        public List<Operand.InstNumber> constArrayVal;

        public boolean isArray() {
            return dimension > 0;
        }
    }

    public static class Function extends Symbol {
        public boolean isVoid;
        public List<Symbol.Var> params = new ArrayList<>();
    }
}

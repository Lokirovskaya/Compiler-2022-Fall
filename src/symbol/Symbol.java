package symbol;

import java.util.ArrayList;
import java.util.List;

public abstract class Symbol {
    String name;
    int lineNumber;

    public static class Var extends Symbol {
        public boolean isConst;
        public int dimension; // 普通变量为 0
        List<Integer> capacity; // 在建立符号表时不填
    }

    public static class Function extends Symbol {
        public boolean isVoid;
        public List<Symbol.Var> params = new ArrayList<>();
    }
}

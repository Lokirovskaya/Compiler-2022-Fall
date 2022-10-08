package symbol;

import error.ErrorList;

import java.util.HashMap;
import java.util.Map;

import static error.Error.ErrorType.IDENTIFIER_DUPLICATE;

public class Table {
    Map<String, Symbol> table = new HashMap<>();
    Table parent;
    public int id;

    // 向当前符号表添加符号
    void addSymbol(Symbol symbol) {
        if (!table.containsKey(symbol.name)) {
            table.put(symbol.name, symbol);
            symbol.selfTable = this;
        }
        else ErrorList.add(IDENTIFIER_DUPLICATE, symbol.lineNumber);
    }
}

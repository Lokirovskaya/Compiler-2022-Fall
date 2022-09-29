package symbol;

import java.util.HashMap;
import java.util.Map;

public class Table {
    Map<String, Symbol> table = new HashMap<>();
    Table parent;
    int id;
}

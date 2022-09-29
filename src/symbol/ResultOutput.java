package symbol;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.List;

class ResultOutput {
    static void output(String filename, List<Table> tableList) throws IOException {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < tableList.size(); i++) {
            Table table = tableList.get(i);
            sb.append(String.format("Table %d, parent %d\n", i, table.parentIndex));
            for (Symbol symbol : table.table.values()) {
                if (symbol instanceof Symbol.Var) {
                    sb.append(String.format("    VAR %s line:%d dim:%d const:%s (%s)\n", symbol.name, symbol.lineNumber, ((Symbol.Var) symbol).dimension, ((Symbol.Var) symbol).isConst, symbol));
                }
                else if (symbol instanceof Symbol.Function) {
                    sb.append(String.format("    FUNC %s line:%d void:%s params:%s\n", symbol.name, symbol.lineNumber, ((Symbol.Function) symbol).isVoid, ((Symbol.Function) symbol).params));
                }
            }
            sb.append('\n');
        }
        Files.write(Paths.get(filename), sb.toString().getBytes(StandardCharsets.UTF_8));
    }
}

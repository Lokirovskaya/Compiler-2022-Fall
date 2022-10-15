import error.ErrorList;
import intercode.Generator;
import intercode.InterCode;
import lexer.Lexer;
import lexer.Token;
import optimizer.Optimizer;
import parser.Parser;
import parser.TreeNode;
import symbol.Symbol;
import symbol.TableBuilder;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.List;
import java.util.Map;

public class Compiler {
    public static void main(String[] args) {
        try {
            String code = new String(Files.readAllBytes(Paths.get("testfile.txt")), StandardCharsets.UTF_8);
            compile(code);
        }
        catch (IOException e) {
            e.printStackTrace();
        }
    }

    private static final boolean DEBUG = true;

    static void compile(String code) throws IOException {
        Lexer lexer = new Lexer(code);
        List<Token> tokenList = lexer.getTokens();
        if (DEBUG) lexer.output("output/lexer.txt");

        ErrorList.init();

        Parser parser = new Parser(tokenList);
        TreeNode root = parser.parse();
        if (DEBUG) parser.output("output/parser.txt", true);

        TableBuilder tableBuilder = new TableBuilder(root);
        Map<Token, Symbol> identSymbolMap = tableBuilder.build();
        if (DEBUG) tableBuilder.output("output/table.txt");

        if (DEBUG) ErrorList.alert();
        // ErrorList.output("error.txt");
        if (ErrorList.size() > 0) return;

        Generator generator = new Generator(root, identSymbolMap);
        InterCode inter = generator.generate();
        inter.output("output/inter.txt");
        Optimizer.Optimize(inter);
        inter.output("output/inter_opt.txt");
    }
}

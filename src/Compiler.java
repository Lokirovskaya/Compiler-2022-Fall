import symbol.ErrorList;
import lexer.Lexer;
import lexer.Token;
import parser.Parser;
import parser.TreeNode;
import symbol.TableBuilder;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.List;

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

    private static final boolean DEBUG = false;

    static void compile(String code) throws IOException {
        Lexer lexer = new Lexer(code);
        List<Token> tokenList = lexer.getTokens();
        if (DEBUG) lexer.output("output/lexer.txt");

        ErrorList.init();

        Parser parser = new Parser(tokenList);
        TreeNode root = parser.parse();
        if (DEBUG) parser.output("output/parser.txt", true);

        TableBuilder tableBuilder = new TableBuilder(root);
        tableBuilder.build();
        if (DEBUG) tableBuilder.output("output/table.txt");

        if (DEBUG) ErrorList.alert();
        ErrorList.output("error.txt");
        if (ErrorList.size() > 0) return;
    }
}

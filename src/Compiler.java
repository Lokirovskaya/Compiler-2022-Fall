import lexer.Token;
import lexer.Lexer;
import parser.Parser;
import parser.TreeDebugger;
import parser.TreeNode;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.List;

public class Compiler {
    public static void main(String[] args) {
        try {
            String code = new String(Files.readAllBytes(Paths.get("testfile.txt")), StandardCharsets.UTF_8);

            Lexer lexer = new Lexer(code);
            List<Token> tokenList = lexer.getTokens();

            Parser parser = new Parser(tokenList);
            TreeNode root = parser.parse();

            TreeDebugger.printTree(root);

            //Files.write(Paths.get("output.txt"), output.getBytes(StandardCharsets.UTF_8));

        }
        catch (IOException e) {
            e.printStackTrace();
        }
    }
}

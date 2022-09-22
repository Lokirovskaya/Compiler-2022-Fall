import lexer.Lexer;
import lexer.Token;
import parser.Parser;
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
//            lexer.output();

            Parser parser = new Parser(tokenList);
            TreeNode root = parser.parse();
            parser.output(true,false);
        }
        catch (IOException e) {
            e.printStackTrace();
        }
        System.out.println("Compile End!");
    }
}

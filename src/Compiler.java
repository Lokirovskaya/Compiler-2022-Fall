import error.ErrorList;
import intercode.Generator;
import intercode.Quaternion;
import lexer.Lexer;
import lexer.Token;
import mips.MipsCoder;
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

    private static final boolean OPTIMIZE_ON = true;

    static void compile(String code) throws IOException {
        Lexer lexer = new Lexer(code);
        List<Token> tokenList = lexer.getTokens();
        // lexer.output("_lexer.txt");

        ErrorList.init();

        Parser parser = new Parser(tokenList);
        TreeNode root = parser.parse();
        // parser.output("_parser.txt", false);

        TableBuilder tableBuilder = new TableBuilder(root);
        Map<Token, Symbol> identSymbolMap = tableBuilder.build();
        // tableBuilder.output("_table.txt");

        ErrorList.alert();
        // ErrorList.output("_error.txt");
        if (ErrorList.size() > 0) return;

        Generator generator = new Generator(root, identSymbolMap);
        List<Quaternion> inter = generator.generate();
        if (OPTIMIZE_ON) generator.optimize();

        generator.output("inter.txt");

        MipsCoder mipsCoder = new MipsCoder(inter);
        mipsCoder.generateMips();
        if (OPTIMIZE_ON) mipsCoder.optimize();

        mipsCoder.output("mips.txt");
    }
}

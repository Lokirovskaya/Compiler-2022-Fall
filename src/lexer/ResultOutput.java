package lexer;

import java.util.List;

public class ResultOutput {
    public static String output(List<Token> tokens) {
        StringBuilder sb = new StringBuilder();
        for (Token t : tokens) {
            sb.append(t.toString());
        }
        return sb.toString();
    }
}

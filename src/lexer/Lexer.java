package lexer;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class Lexer {
    private final String code;
    private List<Token> result;

    public Lexer(String code) {
        this.code = code;
    }

    public List<Token> getTokens() {
        List<String> tokenStrings = getTokenStrings(code);
        result = processTokenStrings(tokenStrings);
        System.out.println("Lexer Done!");
        return result;
    }

    public void output() {
        new ResultOutput(result).output();
    }

    private final String regex = "[a-zA-Z_][a-zA-Z0-9_]*|" + // identifier or keywords
            "[0-9]+|\"[^\n]*?\"|" + // int const | string const
            "//[^\\n]*|/\\*.*?\\*/|" + // comments
            "&&|\\|\\||==|>=|<=|!=|" + // 2-letter operators
            "!|\\+|-|\\*|/|%|&|<|>|=|;|,|\\(|\\)|\\[|]|\\{|}|" + // single operators
            "\n"; // (for line number)
    private final Pattern pattern = Pattern.compile(regex, Pattern.DOTALL);

    private List<String> getTokenStrings(String code) {
        Matcher matcher = pattern.matcher(code);
        List<String> result = new ArrayList<>();
        while (matcher.find()) {
            result.add(matcher.group());
        }
        return result;
    }

    private List<Token> processTokenStrings(List<String> tokenStrings) {
        List<Token> tokens = new ArrayList<>();
        int lineNumber = 1;
        for (String s : tokenStrings) {
            if (s.equals("\n")) {
                lineNumber++;
                continue;
            }
            if (s.startsWith("//")) continue;
            if (s.startsWith("/*")) {
                for (int i = 0; i < s.length(); i++) {
                    if (s.charAt(i) == '\n') lineNumber++;
                }
                continue;
            }

            Token token = new Token();
            token.value = s;
            token.lineNumber = lineNumber;
            token.type = TokenClassify.getTokenType(s);
            tokens.add(token);
        }
        return tokens;
    }
}

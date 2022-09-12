import java.util.HashMap;
import java.util.Map;

public class TokenClassify {
    private static final Map<String, TokenType> tokenNameTypeMap = new HashMap<>();

    static TokenType getTokenType(String tokenString) {
        if (tokenNameTypeMap.containsKey(tokenString))
            return tokenNameTypeMap.get(tokenString);
        else if (ASCIIString.isIdentifier(tokenString))
            return TokenType.IDENTIFIER;
        else if (ASCIIString.isUnsignedInteger(tokenString))
            return TokenType.INTEGER_CONST;
        else if (tokenString.charAt(0) == '"' && tokenString.charAt(tokenString.length() - 1) == '"')
            return TokenType.STRING_CONST;
        else {
            System.err.println("Unknown type token: " + tokenString);
            return TokenType.UNKNOWN;
        }
    }


    static {
        tokenNameTypeMap.put("main", TokenType.MAIN);
        tokenNameTypeMap.put("const", TokenType.CONST);
        tokenNameTypeMap.put("int", TokenType.INT);
        tokenNameTypeMap.put("break", TokenType.BREAK);
        tokenNameTypeMap.put("continue", TokenType.CONTINUE);
        tokenNameTypeMap.put("if", TokenType.IF);
        tokenNameTypeMap.put("else", TokenType.ELSE);
        tokenNameTypeMap.put("while", TokenType.WHILE);
        tokenNameTypeMap.put("getint", TokenType.GETINT);
        tokenNameTypeMap.put("printf", TokenType.PRINTF);
        tokenNameTypeMap.put("return", TokenType.RETURN);
        tokenNameTypeMap.put("void", TokenType.VOID);
        tokenNameTypeMap.put("!", TokenType.NOT);
        tokenNameTypeMap.put("&&", TokenType.AND);
        tokenNameTypeMap.put("||", TokenType.OR);
        tokenNameTypeMap.put("+", TokenType.PLUS);
        tokenNameTypeMap.put("-", TokenType.MINUS);
        tokenNameTypeMap.put("*", TokenType.MULTIPLY);
        tokenNameTypeMap.put("/", TokenType.DIVIDE);
        tokenNameTypeMap.put("%", TokenType.MOD);
        tokenNameTypeMap.put("<", TokenType.LESS);
        tokenNameTypeMap.put("<=", TokenType.LESS_EQUAL);
        tokenNameTypeMap.put(">", TokenType.GREATER);
        tokenNameTypeMap.put(">=", TokenType.GREATER_EQUAL);
        tokenNameTypeMap.put("==", TokenType.EQUAL);
        tokenNameTypeMap.put("!=", TokenType.NOT_EQUAL);
        tokenNameTypeMap.put("=", TokenType.ASSIGN);
        tokenNameTypeMap.put(";", TokenType.SEMICOLON);
        tokenNameTypeMap.put(",", TokenType.COMMA);
        tokenNameTypeMap.put("(", TokenType.LEFT_PAREN);
        tokenNameTypeMap.put(")", TokenType.RIGHT_PAREN);
        tokenNameTypeMap.put("[", TokenType.LEFT_BRACKET);
        tokenNameTypeMap.put("]", TokenType.RIGHT_BRACKET);
        tokenNameTypeMap.put("{", TokenType.LEFT_BRACE);
        tokenNameTypeMap.put("}", TokenType.RIGHT_BRACE);
    }
}

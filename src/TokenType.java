import java.util.HashMap;
import java.util.Map;

enum TokenType {
    IDENTIFIER, INTEGER_CONST, STRING_CONST,
    MAIN, CONST, INT, BREAK, CONTINUE, IF, ELSE, WHILE, GETINT, PRINTF, RETURN, VOID,
    NOT, AND, OR, PLUS, MINUS, MULTIPLY, DIVIDE, MOD,
    LESS, LESS_EQUAL, GREATER, GREATER_EQUAL, EQUAL, NOT_EQUAL,
    ASSIGN, SEMICOLON, COMMA,
    LEFT_PAREN, RIGHT_PAREN, LEFT_BRACKET, RIGHT_BRACKET, LEFT_BRACE, RIGHT_BRACE;

    static TokenType getTokenType(String tokenString) {
        if (tokenNameTypeMap.containsKey(tokenString))
            return tokenNameTypeMap.get(tokenString);
        else if ('0' <= tokenString.charAt(0) && tokenString.charAt(0) <= '9')
            return INTEGER_CONST;
        else if (tokenString.charAt(0) == '"')
            return STRING_CONST;
        else
            return IDENTIFIER;
    }

    private static final Map<String, TokenType> tokenNameTypeMap = new HashMap<>();

    static {
        tokenNameTypeMap.put("main", MAIN);
        tokenNameTypeMap.put("const", CONST);
        tokenNameTypeMap.put("int", INT);
        tokenNameTypeMap.put("break", BREAK);
        tokenNameTypeMap.put("continue", CONTINUE);
        tokenNameTypeMap.put("if", IF);
        tokenNameTypeMap.put("else", ELSE);
        tokenNameTypeMap.put("while", WHILE);
        tokenNameTypeMap.put("getint", GETINT);
        tokenNameTypeMap.put("printf", PRINTF);
        tokenNameTypeMap.put("return", RETURN);
        tokenNameTypeMap.put("void", VOID);
        tokenNameTypeMap.put("!", NOT);
        tokenNameTypeMap.put("&&", AND);
        tokenNameTypeMap.put("||", OR);
        tokenNameTypeMap.put("+", PLUS);
        tokenNameTypeMap.put("-", MINUS);
        tokenNameTypeMap.put("*", MULTIPLY);
        tokenNameTypeMap.put("/", DIVIDE);
        tokenNameTypeMap.put("%", MOD);
        tokenNameTypeMap.put("<", LESS);
        tokenNameTypeMap.put("<=", LESS_EQUAL);
        tokenNameTypeMap.put(">", GREATER);
        tokenNameTypeMap.put(">=", GREATER_EQUAL);
        tokenNameTypeMap.put("==", EQUAL);
        tokenNameTypeMap.put("!=", NOT_EQUAL);
        tokenNameTypeMap.put("=", ASSIGN);
        tokenNameTypeMap.put(";", SEMICOLON);
        tokenNameTypeMap.put(",", COMMA);
        tokenNameTypeMap.put("(", LEFT_PAREN);
        tokenNameTypeMap.put(")", RIGHT_PAREN);
        tokenNameTypeMap.put("[", LEFT_BRACKET);
        tokenNameTypeMap.put("]", RIGHT_BRACKET);
        tokenNameTypeMap.put("{", LEFT_BRACE);
        tokenNameTypeMap.put("}", RIGHT_BRACE);
    }
}

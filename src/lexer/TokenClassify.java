package lexer;

import java.util.HashMap;
import java.util.Map;

import static lexer.Token.TokenType.*;

class TokenClassify {
    static Token.TokenType getTokenType(String tokenValue) {
        if (tokenValueTypeMap.containsKey(tokenValue))
            return tokenValueTypeMap.get(tokenValue);
        else if ('0' <= tokenValue.charAt(0) && tokenValue.charAt(0) <= '9')
            return INT_CONST;
        else if (tokenValue.charAt(0) == '"')
            return STRING_CONST;
        else
            return IDENTIFIER;
    }

    private static final Map<String, Token.TokenType> tokenValueTypeMap = new HashMap<>();

    static {
        tokenValueTypeMap.put("bitand", BITAND);
        tokenValueTypeMap.put("main", MAIN);
        tokenValueTypeMap.put("const", CONST);
        tokenValueTypeMap.put("int", INT);
        tokenValueTypeMap.put("break", BREAK);
        tokenValueTypeMap.put("continue", CONTINUE);
        tokenValueTypeMap.put("if", IF);
        tokenValueTypeMap.put("else", ELSE);
        tokenValueTypeMap.put("while", WHILE);
        tokenValueTypeMap.put("getint", GETINT);
        tokenValueTypeMap.put("printf", PRINTF);
        tokenValueTypeMap.put("return", RETURN);
        tokenValueTypeMap.put("void", VOID);
        tokenValueTypeMap.put("!", NOT);
        tokenValueTypeMap.put("&&", AND);
        tokenValueTypeMap.put("||", OR);
        tokenValueTypeMap.put("+", PLUS);
        tokenValueTypeMap.put("-", MINUS);
        tokenValueTypeMap.put("*", MULTIPLY);
        tokenValueTypeMap.put("/", DIVIDE);
        tokenValueTypeMap.put("%", MOD);
        tokenValueTypeMap.put("<", LESS);
        tokenValueTypeMap.put("<=", LESS_EQUAL);
        tokenValueTypeMap.put(">", GREATER);
        tokenValueTypeMap.put(">=", GREATER_EQUAL);
        tokenValueTypeMap.put("==", EQUAL);
        tokenValueTypeMap.put("!=", NOT_EQUAL);
        tokenValueTypeMap.put("=", ASSIGN);
        tokenValueTypeMap.put(";", SEMICOLON);
        tokenValueTypeMap.put(",", COMMA);
        tokenValueTypeMap.put("(", LEFT_PAREN);
        tokenValueTypeMap.put(")", RIGHT_PAREN);
        tokenValueTypeMap.put("[", LEFT_BRACKET);
        tokenValueTypeMap.put("]", RIGHT_BRACKET);
        tokenValueTypeMap.put("{", LEFT_BRACE);
        tokenValueTypeMap.put("}", RIGHT_BRACE);
    }
}

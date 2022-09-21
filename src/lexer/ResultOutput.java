package lexer;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static lexer.Token.TokenType.*;

public class ResultOutput {
    public static String output(List<Token> tokens) {
        StringBuilder sb = new StringBuilder();
        for (Token t : tokens) {
            sb.append(String.format("%s %s\n", getTokenOutputName(t), t.value));
        }
        return sb.toString();
    }

    public static String getTokenOutputName(Token token) {
        return tokenTypeOutputNameMap.get(token.type);
    }

    private static final Map<Token.TokenType, String> tokenTypeOutputNameMap = new HashMap<>();

    static {
        tokenTypeOutputNameMap.put(IDENTIFIER, "IDENFR");
        tokenTypeOutputNameMap.put(INT_CONST, "INTCON");
        tokenTypeOutputNameMap.put(STRING_CONST, "STRCON");
        tokenTypeOutputNameMap.put(MAIN, "MAINTK");
        tokenTypeOutputNameMap.put(CONST, "CONSTTK");
        tokenTypeOutputNameMap.put(INT, "INTTK");
        tokenTypeOutputNameMap.put(BREAK, "BREAKTK");
        tokenTypeOutputNameMap.put(CONTINUE, "CONTINUETK");
        tokenTypeOutputNameMap.put(IF, "IFTK");
        tokenTypeOutputNameMap.put(ELSE, "ELSETK");
        tokenTypeOutputNameMap.put(WHILE, "WHILETK");
        tokenTypeOutputNameMap.put(GETINT, "GETINTTK");
        tokenTypeOutputNameMap.put(PRINTF, "PRINTFTK");
        tokenTypeOutputNameMap.put(RETURN, "RETURNTK");
        tokenTypeOutputNameMap.put(VOID, "VOIDTK");
        tokenTypeOutputNameMap.put(NOT, "NOT");
        tokenTypeOutputNameMap.put(AND, "AND");
        tokenTypeOutputNameMap.put(OR, "OR");
        tokenTypeOutputNameMap.put(PLUS, "PLUS");
        tokenTypeOutputNameMap.put(MINUS, "MINU");
        tokenTypeOutputNameMap.put(MULTIPLY, "MULT");
        tokenTypeOutputNameMap.put(DIVIDE, "DIV");
        tokenTypeOutputNameMap.put(MOD, "MOD");
        tokenTypeOutputNameMap.put(LESS, "LSS");
        tokenTypeOutputNameMap.put(LESS_EQUAL, "LEQ");
        tokenTypeOutputNameMap.put(GREATER, "GRE");
        tokenTypeOutputNameMap.put(GREATER_EQUAL, "GEQ");
        tokenTypeOutputNameMap.put(EQUAL, "EQL");
        tokenTypeOutputNameMap.put(NOT_EQUAL, "NEQ");
        tokenTypeOutputNameMap.put(ASSIGN, "ASSIGN");
        tokenTypeOutputNameMap.put(SEMICOLON, "SEMICN");
        tokenTypeOutputNameMap.put(COMMA, "COMMA");
        tokenTypeOutputNameMap.put(LEFT_PAREN, "LPARENT");
        tokenTypeOutputNameMap.put(RIGHT_PAREN, "RPARENT");
        tokenTypeOutputNameMap.put(LEFT_BRACKET, "LBRACK");
        tokenTypeOutputNameMap.put(RIGHT_BRACKET, "RBRACK");
        tokenTypeOutputNameMap.put(LEFT_BRACE, "LBRACE");
        tokenTypeOutputNameMap.put(RIGHT_BRACE, "RBRACE");
    }
}

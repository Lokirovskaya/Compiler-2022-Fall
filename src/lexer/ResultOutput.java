package lexer;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static lexer.Token.TokenType.*;

class ResultOutput {
    private final List<Token> result;

    ResultOutput(List<Token> result) {
        this.result = result;
    }

    void output() {
        StringBuilder sb = new StringBuilder();
        for (Token t : result) {
            sb.append(tokenToString(t)).append('\n');
        }
        String str = sb.toString();
        try {
            Files.write(Paths.get("output.txt"), str.getBytes(StandardCharsets.UTF_8));
        }
        catch (IOException e) {
            e.printStackTrace();
        }
    }

    static String tokenToString(Token token) {
        return String.format("%s %s", tokenTypeOutputNameMap.get(token.type), token.value);
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

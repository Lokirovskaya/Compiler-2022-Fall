import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class TokenOutput {
    private static final Map<TokenType, String> tokenTypeOutputNameMap = new HashMap<>();

    static String getOutput(List<Token> tokens) {
        StringBuilder sb = new StringBuilder();
        for (Token token : tokens) {
            sb.append(String.format("%s %s\n", tokenTypeOutputNameMap.get(token.type), token.name));
        }
        return sb.toString();
    }

    static {
        tokenTypeOutputNameMap.put(TokenType.IDENTIFIER, "IDENFR");
        tokenTypeOutputNameMap.put(TokenType.INTEGER_CONST, "INTCON");
        tokenTypeOutputNameMap.put(TokenType.STRING_CONST, "STRCON");
        tokenTypeOutputNameMap.put(TokenType.MAIN, "MAINTK");
        tokenTypeOutputNameMap.put(TokenType.CONST, "CONSTTK");
        tokenTypeOutputNameMap.put(TokenType.INT, "INTTK");
        tokenTypeOutputNameMap.put(TokenType.BREAK, "BREAKTK");
        tokenTypeOutputNameMap.put(TokenType.CONTINUE, "CONTINUETK");
        tokenTypeOutputNameMap.put(TokenType.IF, "IFTK");
        tokenTypeOutputNameMap.put(TokenType.ELSE, "ELSETK");
        tokenTypeOutputNameMap.put(TokenType.WHILE, "WHILETK");
        tokenTypeOutputNameMap.put(TokenType.GETINT, "GETINTTK");
        tokenTypeOutputNameMap.put(TokenType.PRINTF, "PRINTFTK");
        tokenTypeOutputNameMap.put(TokenType.RETURN, "RETURNTK");
        tokenTypeOutputNameMap.put(TokenType.VOID, "VOIDTK");
        tokenTypeOutputNameMap.put(TokenType.NOT, "NOT");
        tokenTypeOutputNameMap.put(TokenType.AND, "AND");
        tokenTypeOutputNameMap.put(TokenType.OR, "OR");
        tokenTypeOutputNameMap.put(TokenType.PLUS, "PLUS");
        tokenTypeOutputNameMap.put(TokenType.MINUS, "MINU");
        tokenTypeOutputNameMap.put(TokenType.MULTIPLY, "MULT");
        tokenTypeOutputNameMap.put(TokenType.DIVIDE, "DIV");
        tokenTypeOutputNameMap.put(TokenType.MOD, "MOD");
        tokenTypeOutputNameMap.put(TokenType.LESS, "LSS");
        tokenTypeOutputNameMap.put(TokenType.LESS_EQUAL, "LEQ");
        tokenTypeOutputNameMap.put(TokenType.GREATER, "GRE");
        tokenTypeOutputNameMap.put(TokenType.GREATER_EQUAL, "GEQ");
        tokenTypeOutputNameMap.put(TokenType.EQUAL, "EQL");
        tokenTypeOutputNameMap.put(TokenType.NOT_EQUAL, "NEQ");
        tokenTypeOutputNameMap.put(TokenType.ASSIGN, "ASSIGN");
        tokenTypeOutputNameMap.put(TokenType.SEMICOLON, "SEMICN");
        tokenTypeOutputNameMap.put(TokenType.COMMA, "COMMA");
        tokenTypeOutputNameMap.put(TokenType.LEFT_PAREN, "LPARENT");
        tokenTypeOutputNameMap.put(TokenType.RIGHT_PAREN, "RPARENT");
        tokenTypeOutputNameMap.put(TokenType.LEFT_BRACKET, "LBRACK");
        tokenTypeOutputNameMap.put(TokenType.RIGHT_BRACKET, "RBRACK");
        tokenTypeOutputNameMap.put(TokenType.LEFT_BRACE, "LBRACE");
        tokenTypeOutputNameMap.put(TokenType.RIGHT_BRACE, "RBRACE");
    }
}

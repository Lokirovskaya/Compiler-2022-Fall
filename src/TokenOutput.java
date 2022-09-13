import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class TokenOutput {
    private static final Map<TokenType, String> tokenTypeOutputMap = new HashMap<>();

    static String output(List<Token> tokens) {
        StringBuilder sb = new StringBuilder();
        for (Token t : tokens) {
            sb.append(String.format("%s %s\n", getTokenOutputTypeName(t), t.name));
        }
        return sb.toString();
    }

    static String getTokenOutputTypeName(Token token) {
        return tokenTypeOutputMap.get(token.type);
    }

    static {
        tokenTypeOutputMap.put(TokenType.IDENTIFIER, "IDENFR");
        tokenTypeOutputMap.put(TokenType.INTEGER_CONST, "INTCON");
        tokenTypeOutputMap.put(TokenType.STRING_CONST, "STRCON");
        tokenTypeOutputMap.put(TokenType.MAIN, "MAINTK");
        tokenTypeOutputMap.put(TokenType.CONST, "CONSTTK");
        tokenTypeOutputMap.put(TokenType.INT, "INTTK");
        tokenTypeOutputMap.put(TokenType.BREAK, "BREAKTK");
        tokenTypeOutputMap.put(TokenType.CONTINUE, "CONTINUETK");
        tokenTypeOutputMap.put(TokenType.IF, "IFTK");
        tokenTypeOutputMap.put(TokenType.ELSE, "ELSETK");
        tokenTypeOutputMap.put(TokenType.WHILE, "WHILETK");
        tokenTypeOutputMap.put(TokenType.GETINT, "GETINTTK");
        tokenTypeOutputMap.put(TokenType.PRINTF, "PRINTFTK");
        tokenTypeOutputMap.put(TokenType.RETURN, "RETURNTK");
        tokenTypeOutputMap.put(TokenType.VOID, "VOIDTK");
        tokenTypeOutputMap.put(TokenType.NOT, "NOT");
        tokenTypeOutputMap.put(TokenType.AND, "AND");
        tokenTypeOutputMap.put(TokenType.OR, "OR");
        tokenTypeOutputMap.put(TokenType.PLUS, "PLUS");
        tokenTypeOutputMap.put(TokenType.MINUS, "MINU");
        tokenTypeOutputMap.put(TokenType.MULTIPLY, "MULT");
        tokenTypeOutputMap.put(TokenType.DIVIDE, "DIV");
        tokenTypeOutputMap.put(TokenType.MOD, "MOD");
        tokenTypeOutputMap.put(TokenType.LESS, "LSS");
        tokenTypeOutputMap.put(TokenType.LESS_EQUAL, "LEQ");
        tokenTypeOutputMap.put(TokenType.GREATER, "GRE");
        tokenTypeOutputMap.put(TokenType.GREATER_EQUAL, "GEQ");
        tokenTypeOutputMap.put(TokenType.EQUAL, "EQL");
        tokenTypeOutputMap.put(TokenType.NOT_EQUAL, "NEQ");
        tokenTypeOutputMap.put(TokenType.ASSIGN, "ASSIGN");
        tokenTypeOutputMap.put(TokenType.SEMICOLON, "SEMICN");
        tokenTypeOutputMap.put(TokenType.COMMA, "COMMA");
        tokenTypeOutputMap.put(TokenType.LEFT_PAREN, "LPARENT");
        tokenTypeOutputMap.put(TokenType.RIGHT_PAREN, "RPARENT");
        tokenTypeOutputMap.put(TokenType.LEFT_BRACKET, "LBRACK");
        tokenTypeOutputMap.put(TokenType.RIGHT_BRACKET, "RBRACK");
        tokenTypeOutputMap.put(TokenType.LEFT_BRACE, "LBRACE");
        tokenTypeOutputMap.put(TokenType.RIGHT_BRACE, "RBRACE");
    }
}

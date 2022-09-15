package lexer;

import java.util.HashMap;
import java.util.Map;

public class Token {
    public String value;
    public int lineNumber;

    @Override
    public String toString() {
        return getOutputTypename(this) + ' ' + value;
    }

    private String getOutputTypename(Token token) {
        if (Token.tokenOutputTypenameMap.containsKey(token.value))
            return Token.tokenOutputTypenameMap.get(token.value);
        else if ('0' <= token.value.charAt(0) && token.value.charAt(0) <= '9')
            return "INTCON";
        else if (token.value.charAt(0) == '"')
            return "STRCON";
        else
            return "IDENFR";
    }

    private static final Map<String, String> tokenOutputTypenameMap = new HashMap<>();

    static {
        tokenOutputTypenameMap.put("main", "MAINTK");
        tokenOutputTypenameMap.put("const", "CONSTTK");
        tokenOutputTypenameMap.put("int", "INTTK");
        tokenOutputTypenameMap.put("break", "BREAKTK");
        tokenOutputTypenameMap.put("continue", "CONTINUETK");
        tokenOutputTypenameMap.put("if", "IFTK");
        tokenOutputTypenameMap.put("else", "ELSETK");
        tokenOutputTypenameMap.put("while", "WHILETK");
        tokenOutputTypenameMap.put("getint", "GETINTTK");
        tokenOutputTypenameMap.put("printf", "PRINTFTK");
        tokenOutputTypenameMap.put("return", "RETURNTK");
        tokenOutputTypenameMap.put("void", "VOIDTK");
        tokenOutputTypenameMap.put("!", "NOT");
        tokenOutputTypenameMap.put("&&", "AND");
        tokenOutputTypenameMap.put("||", "OR");
        tokenOutputTypenameMap.put("+", "PLUS");
        tokenOutputTypenameMap.put("-", "MINU");
        tokenOutputTypenameMap.put("*", "MULT");
        tokenOutputTypenameMap.put("/", "DIV");
        tokenOutputTypenameMap.put("%", "MOD");
        tokenOutputTypenameMap.put("<", "LSS");
        tokenOutputTypenameMap.put("<=", "LEQ");
        tokenOutputTypenameMap.put(">", "GRE");
        tokenOutputTypenameMap.put(">=", "GEQ");
        tokenOutputTypenameMap.put("==", "EQL");
        tokenOutputTypenameMap.put("!=", "NEQ");
        tokenOutputTypenameMap.put("=", "ASSIGN");
        tokenOutputTypenameMap.put(";", "SEMICN");
        tokenOutputTypenameMap.put(",", "COMMA");
        tokenOutputTypenameMap.put("(", "LPARENT");
        tokenOutputTypenameMap.put(")", "RPARENT");
        tokenOutputTypenameMap.put("[", "LBRACK");
        tokenOutputTypenameMap.put("]", "RBRACK");
        tokenOutputTypenameMap.put("{", "LBRACE");
        tokenOutputTypenameMap.put("}", "RBRACE");
    }
}

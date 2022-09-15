package parser;

import lexer.Token;

import static parser.TreeNodeType.*;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;


public class TerminalReader {
    private final List<Terminal> terminalList = new ArrayList<>();
    private int i = 0;

    void init(List<Token> tokenList) {
        terminalList.clear();
        i = 0;
        for (Token token : tokenList) {
            TreeNodeType type = TokenClassify.getTokenType(token);
            terminalList.add(new Terminal(type, token.value, token.lineNumber));
        }
    }

    void next() {
        if (i < terminalList.size()) i++;
    }

    Terminal getNode(int offset) {
        return terminalList.get(i + offset);
    }

    Terminal getNode() {
        return getNode(0);
    }

    TreeNodeType get(int offset) {
        if (i + offset < 0 || i + offset >= terminalList.size())
            return NULL;
        else return terminalList.get(i + offset).type;
    }

    TreeNodeType get() {
        return get(0);
    }
}

class TokenClassify {
    static TreeNodeType getTokenType(Token token) {
        if (tokenValueTypeMap.containsKey(token.value))
            return tokenValueTypeMap.get(token.value);
        else if ('0' <= token.value.charAt(0) && token.value.charAt(0) <= '9')
            return INT_CONST;
        else if (token.value.charAt(0) == '"')
            return STRING_CONST;
        else
            return IDENTIFIER;
    }

    private static final Map<String, TreeNodeType> tokenValueTypeMap = new HashMap<>();

    static {
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

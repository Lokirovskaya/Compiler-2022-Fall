package lexer;

import parser.TreeNode;

public class Token extends TreeNode {
    public String value;
    public int lineNumber;
    public TokenType type;

    @Override
    public boolean isType(Token.TokenType t) {
        return this.type == t;
    }

    public enum TokenType {
        NULL,
        IDENTIFIER, INT_CONST, STRING_CONST,
        MAIN, CONST, INT, BREAK, CONTINUE, IF, ELSE, WHILE, GETINT, PRINTF, RETURN, VOID,
        NOT, AND, OR, PLUS, MINUS, MULTIPLY, DIVIDE, MOD,
        LESS, LESS_EQUAL, GREATER, GREATER_EQUAL, EQUAL, NOT_EQUAL, ASSIGN, SEMICOLON, COMMA,
        LEFT_PAREN, RIGHT_PAREN, LEFT_BRACKET, RIGHT_BRACKET, LEFT_BRACE, RIGHT_BRACE
    }

    @Override
    public String toString() {
        return ResultOutput.tokenToString(this);
    }
}

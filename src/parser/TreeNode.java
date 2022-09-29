package parser;

import lexer.Token;

public abstract class TreeNode {
    public Nonterminal parent;

    public boolean isType(Token.TokenType type) {
        if (this instanceof Nonterminal) return false;
        else return ((Token) this).type == type;
    }

    public boolean isType(Nonterminal.NonterminalType type) {
        if (this instanceof Token) return false;
        else return ((Nonterminal) this).type == type;
    }
}

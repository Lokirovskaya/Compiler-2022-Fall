package parser;

import lexer.Token;

public abstract class TreeNode {
    public Nonterminal parent;

    public boolean isType(Token.TokenType t) {
        if (this instanceof Nonterminal) return false;
        else return ((Token) this).type == t;
    }

    public boolean isType(Nonterminal.NonterminalType n) {
        if (this instanceof Token) return false;
        else return ((Nonterminal) this).type == n;
    }
}

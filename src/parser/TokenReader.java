package parser;

import lexer.Token;

import java.util.List;

class TokenReader {
    private List<Token> tokenList;
    private int i = 0;
    private int checkpoint = 0;

    void init(List<Token> tokenList) {
        this.tokenList = tokenList;
    }

    void next() {
        if (i < tokenList.size()) i++;
    }

    Token readToken() {
        return tokenList.get(i);
    }

    Token readPrevToken() {
        return tokenList.get(i - 1);
    }

    Token.TokenType read(int offset) {
        if (i + offset < 0 || i + offset >= tokenList.size())
            return Token.TokenType.NULL;
        else return tokenList.get(i + offset).type;
    }

    Token.TokenType read() {
        return tokenList.get(i).type;
    }

    // 用于回溯
    void makeCheckpoint() {
        checkpoint = i;
    }

    void loadCheckpoint() {
        i = checkpoint;
    }
}

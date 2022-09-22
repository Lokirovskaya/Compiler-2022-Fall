package parser;

import lexer.Token;

import java.util.List;
import java.util.function.Supplier;

import static lexer.Token.TokenType.*;
import static parser.Nonterminal.NonterminalType.*;

public class Parser implements ParserUtil {
    TreeNode result;

    public Parser(List<Token> tokenList) {
        tokenReader.init(tokenList);
    }

    public TreeNode parse() {
        COMPILE_UNIT();
        result = treeBuilder.getRoot();
        System.out.println("Parser Done!");
        return result;
    }

    public void output(boolean transformToLeftRecursive, boolean outputFullTree) {
        new ResultOutput().output(result, transformToLeftRecursive, outputFullTree);
    }

    private void COMPILE_UNIT() {
        createNonterminal(_COMPILE_UNIT_);
        Supplier<Boolean> isFunctionDefine = () -> (tokenReader.read() == VOID) || (
                tokenReader.read() == INT && tokenReader.read(1) == IDENTIFIER && tokenReader.read(2) == LEFT_PAREN
        );
        Supplier<Boolean> isMainFunctionDefine = () -> tokenReader.read() == INT && tokenReader.read(1) == MAIN;

        while (!isMainFunctionDefine.get()) {
            if (isFunctionDefine.get()) FUNCTION_DEFINE();
            else DECLARE();
        }
        MAIN_FUNCTION_DEFINE();
        endNonterminal();
    }

    private void DECLARE() {
        createNonterminal(_DECLARE_);
        if (tokenReader.read() == CONST)
            CONST_DECLARE();
        else
            VAR_DECLARE();
        endNonterminal();
    }

    private void BASIC_TYPE() {
        createNonterminal(_BASIC_TYPE_);
        consume(INT);
        endNonterminal();
    }

    private void CONST_DECLARE() {
        createNonterminal(_CONST_DECLARE_);
        consume(CONST);
        BASIC_TYPE();
        CONST_DEFINE();
        while (tokenReader.read() == COMMA) {
            consume(COMMA);
            CONST_DEFINE();
        }
        consume(SEMICOLON);
        endNonterminal();
    }

    private void CONST_DEFINE() {
        createNonterminal(_CONST_DEFINE_);
        consume(IDENTIFIER);
        while (tokenReader.read() == LEFT_BRACKET) {
            consume(LEFT_BRACKET);
            CONST_EXPRESSION();
            consume(RIGHT_BRACKET);
        }
        consume(ASSIGN);
        CONST_INIT_VALUE();
        endNonterminal();
    }

    private void CONST_INIT_VALUE() {
        createNonterminal(_CONST_INIT_VALUE_);
        if (tokenReader.read() != LEFT_BRACE) {
            CONST_EXPRESSION();
        }
        else {
            consume(LEFT_BRACE);
            // is '{}' ?
            if (tokenReader.read() == RIGHT_BRACE) consume(RIGHT_BRACE);
            else {
                CONST_INIT_VALUE();
                while (tokenReader.read() == COMMA) {
                    consume(COMMA);
                    CONST_INIT_VALUE();
                }
                consume(RIGHT_BRACE);
            }
        }
        endNonterminal();
    }

    private void VAR_DECLARE() {
        createNonterminal(_VAR_DECLARE_);
        BASIC_TYPE();
        VAR_DEFINE();
        while (tokenReader.read() == COMMA) {
            consume(COMMA);
            VAR_DEFINE();
        }
        consume(SEMICOLON);
        endNonterminal();
    }

    private void VAR_DEFINE() {
        createNonterminal(_VAR_DEFINE_);
        consume(IDENTIFIER);
        while (tokenReader.read() == LEFT_BRACKET) {
            consume(LEFT_BRACKET);
            CONST_EXPRESSION();
            consume(RIGHT_BRACKET);
        }
        if (tokenReader.read() == ASSIGN) {
            consume(ASSIGN);
            VAR_INIT_VALUE();
        }
        endNonterminal();
    }

    private void VAR_INIT_VALUE() {
        createNonterminal(_VAR_INIT_VALUE_);
        if (tokenReader.read() != LEFT_BRACE) {
            EXPRESSION();
        }
        else {
            consume(LEFT_BRACE);
            // is not '{}' ?
            if (tokenReader.read() != RIGHT_BRACE) {
                VAR_INIT_VALUE();
                while (tokenReader.read() == COMMA) {
                    consume(COMMA);
                    VAR_INIT_VALUE();
                }
            }
            consume(RIGHT_BRACE);
        }
        endNonterminal();
    }

    private void FUNCTION_DEFINE() {
        createNonterminal(_FUNCTION_DEFINE_);
        FUNCTION_TYPE();
        consume(IDENTIFIER);
        consume(LEFT_PAREN);
        // is not '()' ?
        if (tokenReader.read() != RIGHT_PAREN)
            FUNCTION_DEFINE_PARAM_LIST();
        consume(RIGHT_PAREN);
        BLOCK();
        endNonterminal();
    }

    private void MAIN_FUNCTION_DEFINE() {
        createNonterminal(_MAIN_FUNCTION_DEFINE_);
        consume(INT);
        consume(MAIN);
        consume(LEFT_PAREN);
        consume(RIGHT_PAREN);
        BLOCK();
        endNonterminal();
    }

    private void FUNCTION_TYPE() {
        createNonterminal(_FUNCTION_TYPE_);
        consume(VOID, INT);
        endNonterminal();
    }

    private void FUNCTION_DEFINE_PARAM_LIST() {
        createNonterminal(_FUNCTION_DEFINE_PARAM_LIST_);
        FUNCTION_DEFINE_PARAM();
        while (tokenReader.read() == COMMA) {
            consume(COMMA);
            FUNCTION_DEFINE_PARAM();
        }
        endNonterminal();
    }

    private void FUNCTION_DEFINE_PARAM() {
        createNonterminal(_FUNCTION_DEFINE_PARAM_);
        BASIC_TYPE();
        consume(IDENTIFIER);
        if (tokenReader.read() == LEFT_BRACKET) {
            consume(LEFT_BRACKET);
            consume(RIGHT_BRACKET);
            while (tokenReader.read() == LEFT_BRACKET) {
                consume(LEFT_BRACKET);
                CONST_EXPRESSION();
                consume(RIGHT_BRACKET);
            }
        }
        endNonterminal();
    }

    private void FUNCTION_CALL_PARAM_LIST() {
        createNonterminal(_FUNCTION_CALL_PARAM_LIST_);
        EXPRESSION();
        while (tokenReader.read() == COMMA) {
            consume(COMMA);
            EXPRESSION();
        }
        endNonterminal();
    }

    private void BLOCK() {
        createNonterminal(_BLOCK_);
        consume(LEFT_BRACE);
        while (tokenReader.read() != RIGHT_BRACE) {
            BLOCK_ITEM();
        }
        consume(RIGHT_BRACE);
        endNonterminal();
    }

    private void BLOCK_ITEM() {
        createNonterminal(_BLOCK_ITEM_);
        Supplier<Boolean> isDeclare = () -> (tokenReader.read() == CONST) || (
                tokenReader.read() == INT && tokenReader.read(1) == IDENTIFIER && tokenReader.read(2) != LEFT_PAREN
        );
        if (isDeclare.get()) DECLARE();
        else STATEMENT();
        endNonterminal();
    }

    private void STATEMENT() {
        createNonterminal(_STATEMENT_);
        // 一直读到分号为止，如果之间发现了 ASSIGN，就是赋值语句
        int i = 0;
        boolean isAssign = false, isGetint = false;
        while (tokenReader.read(i) != SEMICOLON && tokenReader.read(i) != NULL) {
            if (tokenReader.read(i) == ASSIGN) {
                isAssign = true;
                if (tokenReader.read(i + 1) == GETINT)
                    isGetint = true;
                break;
            }
            i++;
        }

        // 'if' '(' Cond ')' Stmt [ 'else' Stmt ]
        if (tokenReader.read() == IF) {
            consume(IF);
            consume(LEFT_PAREN);
            CONDITION();
            consume(RIGHT_PAREN);
            STATEMENT();
            if (tokenReader.read() == ELSE) {
                consume(ELSE);
                STATEMENT();
            }
        }
        // 'while' '(' Cond ')' Stmt
        else if (tokenReader.read() == WHILE) {
            consume(WHILE);
            consume(LEFT_PAREN);
            CONDITION();
            consume(RIGHT_PAREN);
            STATEMENT();
        }
        // 'break' ';' | 'continue' ';'
        else if (tokenReader.read() == BREAK || tokenReader.read() == CONTINUE) {
            consume(BREAK, CONTINUE);
            consume(SEMICOLON);
        }
        // 'return' [Exp] ';'
        else if (tokenReader.read() == RETURN) {
            consume(RETURN);
            if (tokenReader.read() != SEMICOLON)
                EXPRESSION();
            consume(SEMICOLON);
        }
        // 'printf' '(' FormatString { ',' Exp } ')' ';'
        else if (tokenReader.read() == PRINTF) {
            consume(PRINTF);
            consume(LEFT_PAREN);
            consume(STRING_CONST);
            while (tokenReader.read() == COMMA) {
                consume(COMMA);
                EXPRESSION();
            }
            consume(RIGHT_PAREN);
            consume(SEMICOLON);
        }
        // Block
        else if (tokenReader.read() == LEFT_BRACE) {
            BLOCK();
        }
        // LVal '=' 'getint' '(' ')' ';'
        else if (isGetint) {
            LEFT_VALUE();
            consume(ASSIGN);
            consume(GETINT);
            consume(LEFT_PAREN);
            consume(RIGHT_PAREN);
            consume(SEMICOLON);
        }
        // LVal '=' Exp ';'
        else if (isAssign) {
            LEFT_VALUE();
            consume(ASSIGN);
            EXPRESSION();
            consume(SEMICOLON);
        }
        // [Exp] ';'
        else {
            if (tokenReader.read() != SEMICOLON)
                EXPRESSION();
            consume(SEMICOLON);
        }
        endNonterminal();
    }

    private void EXPRESSION() {
        createNonterminal(_EXPRESSION_);
        ADD_EXPRESSION();
        endNonterminal();
    }

    private void CONST_EXPRESSION() {
        createNonterminal(_CONST_EXPRESSION_);
        ADD_EXPRESSION();
        endNonterminal();
    }

    private void ADD_EXPRESSION() {
        // 消除左递归：AddExp → MulExp { ('+'|'-') MulExp }
        createNonterminal(_ADD_EXPRESSION_);
        MULTIPLY_EXPRESSION();
        while (tokenReader.read() == PLUS || tokenReader.read() == MINUS) {
            consume(PLUS, MINUS);
            MULTIPLY_EXPRESSION();
        }
        endNonterminal();
    }

    private void MULTIPLY_EXPRESSION() {
        // 消除左递归：MulExp → UnaryExp { ('*'|'/'|'%') UnaryExp }
        createNonterminal(_MULTIPLY_EXPRESSION_);
        UNARY_EXPRESSION();
        while (tokenReader.read() == MULTIPLY || tokenReader.read() == DIVIDE || tokenReader.read() == MOD) {
            consume(MULTIPLY, DIVIDE, MOD);
            UNARY_EXPRESSION();
        }
        endNonterminal();
    }

    private void UNARY_EXPRESSION() {
        createNonterminal(_UNARY_EXPRESSION_);
        // Ident '(' [FuncRParams] ')'
        if (tokenReader.read() == IDENTIFIER && tokenReader.read(1) == LEFT_PAREN) {
            consume(IDENTIFIER);
            consume(LEFT_PAREN);
            if (tokenReader.read() != RIGHT_PAREN) {
                FUNCTION_CALL_PARAM_LIST();
            }
            consume(RIGHT_PAREN);
        }
        // UnaryOp UnaryExp
        else if (tokenReader.read() == PLUS || tokenReader.read() == MINUS || tokenReader.read() == NOT) {
            UNARY_OPERATOR();
            UNARY_EXPRESSION();
        }
        // PrimaryExp
        else PRIMARY_EXPRESSION();
        endNonterminal();
    }

    private void UNARY_OPERATOR() {
        createNonterminal(_UNARY_OPERATOR_);
        consume(PLUS, MINUS, NOT); // 正、负、非
        endNonterminal();
    }

    private void PRIMARY_EXPRESSION() {
        createNonterminal(_PRIMARY_EXPRESSION_);
        // '(' Exp ')'
        if (tokenReader.read() == LEFT_PAREN) {
            consume(LEFT_PAREN);
            EXPRESSION();
            consume(RIGHT_PAREN);
        }
        // Number
        else if (tokenReader.read() == INT_CONST) {
            NUMBER();
        }
        // LVal
        else LEFT_VALUE();
        endNonterminal();
    }

    private void CONDITION() {
        createNonterminal(_CONDITION_);
        LOGIC_OR_EXPRESSION();
        endNonterminal();
    }

    private void LOGIC_OR_EXPRESSION() {
        // 消除左递归：LAndExp { '||' LAndExp }
        createNonterminal(_LOGIC_OR_EXPRESSION_);
        LOGIC_AND_EXPRESSION();
        while (tokenReader.read() == OR) {
            consume(OR);
            LOGIC_AND_EXPRESSION();
        }
        endNonterminal();
    }

    private void LOGIC_AND_EXPRESSION() {
        // 消除左递归：EqExp { '&&' EqExp }
        createNonterminal(_LOGIC_AND_EXPRESSION_);
        EQUAL_EXPRESSION();
        while (tokenReader.read() == AND) {
            consume(AND);
            EQUAL_EXPRESSION();
        }
        endNonterminal();
    }

    private void EQUAL_EXPRESSION() {
        // 消除左递归：RelExp { ('=='|'!=') RelExp }
        createNonterminal(_EQUAL_EXPRESSION_);
        RELATION_EXPRESSION();
        while (tokenReader.read() == EQUAL || tokenReader.read() == NOT_EQUAL) {
            consume(EQUAL, NOT_EQUAL);
            RELATION_EXPRESSION();
        }
        endNonterminal();
    }

    private void RELATION_EXPRESSION() {
        // 消除左递归：AddExp { ('<'|'>'|'<='|'>=') AddExp }
        createNonterminal(_RELATION_EXPRESSION_);
        ADD_EXPRESSION();
        while (tokenReader.read() == LESS || tokenReader.read() == LESS_EQUAL ||
                tokenReader.read() == GREATER || tokenReader.read() == GREATER_EQUAL) {
            consume(LESS, LESS_EQUAL, GREATER, GREATER_EQUAL);
            ADD_EXPRESSION();
        }
        endNonterminal();
    }

    private void LEFT_VALUE() {
        createNonterminal(_LEFT_VALUE_);
        consume(IDENTIFIER);
        while (tokenReader.read() == LEFT_BRACKET) {
            consume(LEFT_BRACKET);
            EXPRESSION();
            consume(RIGHT_BRACKET);
        }
        endNonterminal();
    }

    private void NUMBER() {
        createNonterminal(_NUMBER_);
        consume(INT_CONST);
        endNonterminal();
    }
}

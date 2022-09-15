package parser;

import lexer.Token;

import static parser.TreeNodeType.*;

import java.util.List;
import java.util.function.Supplier;


public class Parser {
    private final TerminalReader tr = new TerminalReader();
    private final TreeBuilder treeBuilder = new TreeBuilder();

    public Parser(List<Token> tokenList) {
        tr.init(tokenList);
    }

    public TreeNode parse() {
        parseCompileUnit();
        return treeBuilder.getRoot();
    }

    // 判断当前终结符号是否和指定类型匹配，若匹配，消耗当前终结符号，并且把它加入到树中
    private void consumeCurrentTerminal(TreeNodeType judge) {
        Terminal node = tr.getNode();
        if (node.type == judge) {
            treeBuilder.addNode(tr.getNode());
            tr.next();
        }
        else System.err.printf("Parsing error, current token %s, at line %d\n", node.value, node.lineNumber);
    }

    // 创建一个非终结符号，作为当前节点的子节点，并将树的指针指向它
    private void createNonterminal(TreeNodeType type) {
        Nonterminal node = new Nonterminal(type);
        treeBuilder.addNode(node);
        treeBuilder.moveTo(node);
    }

    // 结束当前非终结符号的解析，将树的指针重新指向父元素
    private void endNonterminal() {
        treeBuilder.moveUp();
    }

    private void parseCompileUnit() {
        createNonterminal(_COMPILE_UNIT_);
        Supplier<Boolean> isDeclaration = () -> (tr.get() == CONST) || (
                tr.get() == INT && tr.get(1) == IDENTIFIER && (tr.get(2) == ASSIGN || tr.get(2) == SEMICOLON)
        );
        Supplier<Boolean> isFunctionDefinition = () -> (tr.get() == VOID) || (
                tr.get() == INT && tr.get(1) == IDENTIFIER && tr.get(2) == LEFT_PAREN
        );

        while (isDeclaration.get()) {
            parseDeclaration();
        }
        while (isFunctionDefinition.get()) {
            parseFunctionDefinition();
        }
        parseMainFunctionDefinition();
    }

    private void parseDeclaration() {
        createNonterminal(_DECLARATION_);
        if (tr.get() == CONST)
            parseConstDeclaration();
        else
            parseVarDeclaration();
        endNonterminal();
    }

    private void parseBasicType() {
        createNonterminal(_BASIC_TYPE_);
        consumeCurrentTerminal(INT);
        endNonterminal();
    }

    private void parseConstDeclaration() {
        createNonterminal(_CONST_DECLARATION_);
        consumeCurrentTerminal(CONST);
        parseBasicType();
        parseConstDefinition();
        while (tr.get() == COMMA) {
            consumeCurrentTerminal(COMMA);
            parseConstDefinition();
        }
        consumeCurrentTerminal(SEMICOLON);
        endNonterminal();
    }

    private void parseConstDefinition() {
        createNonterminal(_CONST_DEFINITION_);
        consumeCurrentTerminal(IDENTIFIER);
        while (tr.get() == LEFT_BRACKET) {
            consumeCurrentTerminal(LEFT_BRACKET);
            parseConstExpression();
            consumeCurrentTerminal(RIGHT_BRACKET);
        }
        consumeCurrentTerminal(ASSIGN);
        parseConstInitValue();
        endNonterminal();
    }

    private void parseConstInitValue() {
        createNonterminal(_CONST_INIT_VALUE_);
        parseConstExpression();
        // todo: {}{}
        endNonterminal();
    }

    private void parseVarDeclaration() {
        createNonterminal(_VAR_DECLARATION_);
        parseBasicType();
        parseVarDefinition();
        while (tr.get() == COMMA) {
            consumeCurrentTerminal(COMMA);
            parseVarDefinition();
        }
        consumeCurrentTerminal(SEMICOLON);
        endNonterminal();
    }

    private void parseVarDefinition() {
        createNonterminal(_VAR_DEFINITION_);
        consumeCurrentTerminal(IDENTIFIER);
        while (tr.get() == LEFT_BRACKET) {
            consumeCurrentTerminal(LEFT_BRACKET);
            parseConstExpression();
            consumeCurrentTerminal(RIGHT_BRACKET);
        }
        if (tr.get() == ASSIGN) {
            consumeCurrentTerminal(ASSIGN);
            parseVarInitValue();
        }
        endNonterminal();
    }

    private void parseVarInitValue() {
        createNonterminal(_VAR_INIT_VALUE_);
        // todo: correct it
        parseConstExpression();
        endNonterminal();
    }



    private void parseFunctionDefinition() {
        createNonterminal(_FUNCTION_DEFINITION_);
        endNonterminal();
    }

    private void parseMainFunctionDefinition() {
        createNonterminal(_MAIN_FUNCTION_DEFINITION_);
        endNonterminal();
    }



    private void parseConstExpression() {
        createNonterminal(_CONST_EXPRESSION_);
        // todo: correct const exp parser
        consumeCurrentTerminal(INT_CONST);
        endNonterminal();
    }



}

package parser;

import lexer.Token;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

public interface ParserUtil {
    TokenReader tokenReader = new TokenReader();
    TreeBuilder treeBuilder = new TreeBuilder();

    // 消耗当前 token，判断当前 token 是否和指定类型匹配，不匹配则报错
    default void consume(Token.TokenType judge) {
        Token token = tokenReader.readToken();
        if (token.type == judge) {
            treeBuilder.addNode(token);
        }
        else {
            System.err.printf("Unexpected token '%s' at line %d when parsing %s, expected %s\n",
                    token.value, token.lineNumber,
                    treeBuilder.getCurrent().type.name(), judge.name());
        }
        tokenReader.next();
    }

    // 也是消耗并判断当前 token，但是有多个匹配可能
    default void consume(Token.TokenType... judges) {
        Token token = tokenReader.readToken();
        boolean match = false;
        for (Token.TokenType judge : judges) {
            if (token.type == judge) {
                match = true;
                break;
            }
        }
        if (match) {
            treeBuilder.addNode(token);
        }
        else {
            System.err.printf("Unexpected token '%s' at line %d when parsing %s, expected %s\n",
                    token.value, token.lineNumber,
                    treeBuilder.getCurrent().type.name(),
                    Arrays.stream(judges).map(Enum::name).collect(Collectors.toList()));
        }
        tokenReader.next();
    }

    // 创建一个非终结符号，作为当前节点的子节点，并将树的指针指向它
    default void createNonterminal(Nonterminal.NonterminalType type) {
        Nonterminal node = new Nonterminal();
        node.type = type;
        treeBuilder.addNode(node);
        treeBuilder.moveTo(node);
    }

    // 结束当前非终结符号的解析，将树的指针重新指向父元素
    default void endNonterminal() {
        treeBuilder.moveUp();
    }
}

class TokenReader {
    private List<Token> tokenList;
    private int i = 0;

    void init(List<Token> tokenList) {
        this.tokenList = tokenList;
    }

    void next() {
        if (i < tokenList.size()) i++;
    }

    Token readToken() {
        return tokenList.get(i);
    }

    Token.TokenType read(int offset) {
        if (i + offset < 0 || i + offset >= tokenList.size())
            return Token.TokenType.NULL;
        else return tokenList.get(i + offset).type;
    }

    Token.TokenType read() {
        return tokenList.get(i).type;
    }
}

class TreeBuilder {
    private Nonterminal root = null;
    private Nonterminal current = null; // 指针不应在终结节点上

    // 添加新节点，作为当前节点的子节点
    void addNode(TreeNode node) {
        node.parent = current;
        if (root == null) {
            assert node instanceof Nonterminal;
            root = (Nonterminal) node;
        }
        else current.children.add(node);
    }

    // 移动树的指针，不要移动到终结节点
    void moveTo(Nonterminal node) {
        current = node;
    }

    // 将当前节点移动回它的父节点
    void moveUp() {
        if (current.parent != null) moveTo(current.parent);
    }

    Nonterminal getRoot() {
        return root;
    }

    Nonterminal getCurrent() {
        return current;
    }

    // 将消除的左递归在树上复原
    // 只针对 L ::= R{OR} 还原为 L ::= R | LOR
    //     L             L
    //   R  {OR}  =>   L'  OR       【 L' 是新节点，{OR}' 是减少最后一个 OR 的 {OR} 】
    //               R {OR}'
    void transformToLeftRecursive() {
        runTransform(root);
    }

    private void runTransform(TreeNode p) {
        if (p instanceof Nonterminal) {
            Nonterminal L = (Nonterminal) p;
            switch (L.type) {
                case _MULTIPLY_EXPRESSION_:
                case _ADD_EXPRESSION_:
                case _RELATION_EXPRESSION_:
                case _EQUAL_EXPRESSION_:
                case _LOGIC_AND_EXPRESSION_:
                case _LOGIC_OR_EXPRESSION_:
                    // 如果 {OR} 还有内容，就需要转换
                    if (L.children.size() > 1) {
                        int size = L.children.size();
                        assert size % 2 == 1;
                        Nonterminal R = (Nonterminal) L.children.get(0);
                        List<TreeNode> OR = new ArrayList<>(L.children.subList(size - 2, size));
                        Nonterminal L_ = new Nonterminal();
                        L_.type = L.type;
                        L_.children.add(R); // L'.left = R
                        L_.children.addAll(L.children.subList(1, size - 2)); // L'.right = {OR}'
                        L.children = new ArrayList<>();
                        L.children.add(L_); // L.left = L'
                        L.children.addAll(OR); // L.right = OR
                        // set parent
                        for (TreeNode t : L.children) t.parent = L;
                        for (TreeNode t : L_.children) t.parent = L_;
                    }
                    break;
            }
            // 前序遍历
            for (TreeNode next : L.children) {
                runTransform(next);
            }
        }
    }
}

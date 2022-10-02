package parser;

import java.util.ArrayList;
import java.util.List;

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
                        Nonterminal L_ = new Nonterminal(L.type);
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

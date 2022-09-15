package parser;

public class TreeBuilder {
    private TreeNode root = null;
    private TreeNode current = null;

    // 添加新节点，作为当前节点的子节点
    void addNode(TreeNode node) {
        node.parent = current;
        if (root == null) root = node;
        else ((Nonterminal) current).children.add(node);
    }

    // 移动树的指针，不要移动到终结节点
    void moveTo(TreeNode node) {
        assert node instanceof Nonterminal;
        current = node;
    }

    // 将当前节点移动回它的父节点
    void moveUp() {
        moveTo(current.parent);
    }

    TreeNode getRoot() {
        return root;
    }
}

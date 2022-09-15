package parser;

public abstract class TreeNode {
    TreeNodeType type;
    TreeNode parent;  // 所有节点都有 parent，但是只有非终结节点有 children
}

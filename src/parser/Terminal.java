package parser;

public class Terminal extends TreeNode {
    String value;
    int lineNumber;

    Terminal(TreeNodeType type, String value, int lineNumber) {
        super.type = type;
        this.value = value;
        this.lineNumber = lineNumber;
    }
}

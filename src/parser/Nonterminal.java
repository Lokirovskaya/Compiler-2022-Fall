package parser;

import java.util.ArrayList;
import java.util.List;

public class Nonterminal extends TreeNode {
    List<TreeNode> children = new ArrayList<>();

    Nonterminal(TreeNodeType type) {
        super.type = type;
    }
}

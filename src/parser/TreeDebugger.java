package parser;

public class TreeDebugger {
    public static void printTree(TreeNode root) {
        run(root, 0);
    }

    private static void printNode(TreeNode node, int layer) {
        for (int i = 0; i < layer; i++) System.out.print("    ");
        if (node instanceof Terminal)
            System.out.printf("%s [%d]",((Terminal) node).value, ((Terminal) node).lineNumber);
        else
            System.out.printf("<%s>", ((Nonterminal) node).type.name());
        System.out.print("\n");
    }

    private static void run(TreeNode p, int layer) {
        printNode(p, layer);
        if (p instanceof Nonterminal) {
            for (TreeNode next : ((Nonterminal) p).children) {
                run(next, layer+1);
            }
        }
    }
}

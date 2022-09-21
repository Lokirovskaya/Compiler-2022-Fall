package parser;

import lexer.Token;

public class TreeDebugger {
    public static void printTree(TreeNode root) {
        run(root, 0);
    }

    private static void printNodeStart(TreeNode node, int layer) {
        for (int i = 0; i < layer; i++) System.out.print("  ");
        if (node instanceof Token)
            System.out.printf("'%s' [%d]", ((Token) node).value, ((Token) node).lineNumber);
        else
            System.out.printf("<%s>", ((Nonterminal) node).type.name());
        System.out.print("\n");
    }

    private static void printNodeEnd(TreeNode node, int layer) {
        if (node instanceof Nonterminal) {
            for (int i = 0; i < layer; i++) System.out.print("  ");
            System.out.printf("</%s>\n", ((Nonterminal) node).type.name());
        }
    }

    private static void run(TreeNode p, int layer) {
        printNodeStart(p, layer);
        if (p instanceof Nonterminal) {
            for (TreeNode next : ((Nonterminal) p).children) {
                run(next, layer + 1);
            }
        }
        printNodeEnd(p, layer);
    }
}

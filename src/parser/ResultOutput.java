package parser;

import lexer.Token;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;

import static parser.Nonterminal.NonterminalType.*;

class ResultOutput {
    private boolean outputFullTree;
    private final StringBuilder sb = new StringBuilder();

    public void output(TreeNode root, boolean outputFullTree) {
        this.outputFullTree = outputFullTree;

        run(root, 0);

        String str = sb.toString();
        try {
            Files.write(Paths.get("output.txt"), str.getBytes(StandardCharsets.UTF_8));
        }
        catch (IOException e) {
            e.printStackTrace();
        }
    }

    // 前序遍历一棵树，和递归下降顺序相同
    private void run(TreeNode p, int layer) {
        printNodeStart(p, layer);
        if (p instanceof Nonterminal) {
            for (TreeNode next : ((Nonterminal) p).children) {
                run(next, layer + 1);
            }
        }
        printNodeEnd(p, layer);
    }

    private void printNodeStart(TreeNode node, int layer) {
        if (outputFullTree) {
            for (int i = 0; i < layer; i++) sb.append("  ");
            if (node instanceof Token)
                sb.append(String.format("'%s' [%d]\n", ((Token) node).value, ((Token) node).lineNumber));
            else
                sb.append(String.format("<%s>\n", ((Nonterminal) node).type.name()));
        }
        else {
            if (node instanceof Token)
                sb.append(node).append('\n');
        }
    }

    private void printNodeEnd(TreeNode node, int layer) {
        if (node instanceof Nonterminal) {
            if (outputFullTree) {
                for (int i = 0; i < layer; i++) sb.append("  ");
                sb.append(String.format("</%s>\n", ((Nonterminal) node).type.name()));
            }
            else {
                Nonterminal.NonterminalType type = ((Nonterminal) node).type;
                switch (type) {
                    case _BLOCK_ITEM_:
                    case _DECLARE_:
                    case _BASIC_TYPE_:
                        return;
                }
                sb.append(String.format("<%s>\n",
                        nonterminalTypeOutputNameMap.get(type)));
            }
        }
    }

    private static final Map<Nonterminal.NonterminalType, String> nonterminalTypeOutputNameMap = new HashMap<>();

    static {
        nonterminalTypeOutputNameMap.put(_COMPILE_UNIT_, "CompUnit");
        nonterminalTypeOutputNameMap.put(_DECLARE_, "Decl");
        nonterminalTypeOutputNameMap.put(_BASIC_TYPE_, "BType");
        nonterminalTypeOutputNameMap.put(_CONST_DECLARE_, "ConstDecl");
        nonterminalTypeOutputNameMap.put(_CONST_DEFINE_, "ConstDef");
        nonterminalTypeOutputNameMap.put(_CONST_INIT_VALUE_, "ConstInitVal");
        nonterminalTypeOutputNameMap.put(_VAR_DECLARE_, "VarDecl");
        nonterminalTypeOutputNameMap.put(_VAR_DEFINE_, "VarDef");
        nonterminalTypeOutputNameMap.put(_VAR_INIT_VALUE_, "InitVal");
        nonterminalTypeOutputNameMap.put(_FUNCTION_DEFINE_, "FuncDef");
        nonterminalTypeOutputNameMap.put(_MAIN_FUNCTION_DEFINE_, "MainFuncDef");
        nonterminalTypeOutputNameMap.put(_FUNCTION_TYPE_, "FuncType");
        nonterminalTypeOutputNameMap.put(_FUNCTION_DEFINE_PARAM_LIST_, "FuncFParams");
        nonterminalTypeOutputNameMap.put(_FUNCTION_DEFINE_PARAM_, "FuncFParam");
        nonterminalTypeOutputNameMap.put(_BLOCK_, "Block");
        nonterminalTypeOutputNameMap.put(_BLOCK_ITEM_, "BlockItem");
        nonterminalTypeOutputNameMap.put(_STATEMENT_, "Stmt");
        nonterminalTypeOutputNameMap.put(_EXPRESSION_, "Exp");
        nonterminalTypeOutputNameMap.put(_CONDITION_, "Cond");
        nonterminalTypeOutputNameMap.put(_LEFT_VALUE_, "LVal");
        nonterminalTypeOutputNameMap.put(_PRIMARY_EXPRESSION_, "PrimaryExp");
        nonterminalTypeOutputNameMap.put(_NUMBER_, "Number");
        nonterminalTypeOutputNameMap.put(_UNARY_EXPRESSION_, "UnaryExp");
        nonterminalTypeOutputNameMap.put(_UNARY_OPERATOR_, "UnaryOp");
        nonterminalTypeOutputNameMap.put(_FUNCTION_CALL_PARAM_LIST_, "FuncRParams");
        nonterminalTypeOutputNameMap.put(_MULTIPLY_EXPRESSION_, "MulExp");
        nonterminalTypeOutputNameMap.put(_ADD_EXPRESSION_, "AddExp");
        nonterminalTypeOutputNameMap.put(_RELATION_EXPRESSION_, "RelExp");
        nonterminalTypeOutputNameMap.put(_EQUAL_EXPRESSION_, "EqExp");
        nonterminalTypeOutputNameMap.put(_LOGIC_AND_EXPRESSION_, "LAndExp");
        nonterminalTypeOutputNameMap.put(_LOGIC_OR_EXPRESSION_, "LOrExp");
        nonterminalTypeOutputNameMap.put(_CONST_EXPRESSION_, "ConstExp");
    }
}

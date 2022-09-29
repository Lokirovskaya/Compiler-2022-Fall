package symbol;

import parser.Nonterminal;
import parser.TreeNode;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import static lexer.Token.TokenType.*;
import static parser.Nonterminal.NonterminalType.*;

public class TableBuilder {
    // table 树的所有节点
    private final List<Table> tableList = new ArrayList<>();
    private Table current = null;
    private Table root;
    private final TreeNode syntaxTreeRoot;

    public TableBuilder(TreeNode syntaxTreeRoot) {
        this.syntaxTreeRoot = syntaxTreeRoot;
    }

    public List<Table> build() {
        createTable(); // root 表
        root = current;
        runSyntaxTree(syntaxTreeRoot);
        return tableList;
    }

    public void output(String filename) throws IOException {
        ResultOutput.output(filename, tableList);
    }

    // FuncDef → FuncType Ident '(' [FuncFParams] ')' Block
    // 请将多个 FuncFParam 加入此 function 的 params 中
    // 当它不为 null 时，阻止一次 Block 创建新符号表
    private Symbol.Function currentFunctionSymbol = null;

    private void runSyntaxTree(TreeNode _p) {
        if (_p instanceof Nonterminal) {
            Nonterminal p = (Nonterminal) _p;
            TreeNode firstChild = p.children.get(0);
            switch (p.type) {
                // 建符号表
                case _BLOCK_:
                    if (currentFunctionSymbol == null)
                        createTable();
                    currentFunctionSymbol = null;
                    break;
                case _FUNCTION_DEFINE_:
                case _MAIN_FUNCTION_DEFINE_:
                    currentFunctionSymbol = TableUtil.readFunctionDefine(p);
                    addSymbol(currentFunctionSymbol);
                    createTable();
                    if (!currentFunctionSymbol.isVoid) {
                        TableUtil.checkReturnOfIntFunction(p);
                    }
                    break;
                case _VAR_DEFINE_:
                case _CONST_DEFINE_:
                    Symbol.Var var = TableUtil.readVarDefine(p);
                    addSymbol(var);
                    break;
                case _FUNCTION_DEFINE_PARAM_:
                    assert currentFunctionSymbol != null;
                    Symbol.Var param = TableUtil.readVarDefine(p);
                    currentFunctionSymbol.params.add(param);
                    addSymbol(param);
                    break;

                // 错误处理
                case _STATEMENT_:
                    // printf
                    if (firstChild.isType(PRINTF)) {
                        TableUtil.checkFormatString(p);
                    }
                    break;
                case _LEFT_VALUE_:
                    TableUtil.checkUndefineVar(p, current);
                    break;
                case _UNARY_EXPRESSION_:
                    // function call
                    if (firstChild.isType(IDENTIFIER)) {
                        TableUtil.checkFunctionCall(p, root);
                    }
                    break;
            }

            for (TreeNode next : p.children) {
                runSyntaxTree(next);
            }

            // exit
            if (p.type == _BLOCK_) moveUp();
        }
    }

    // 新建符号表，作为当前表的子节点，然后将指针指向它
    private void createTable() {
        Table table = new Table();
        tableList.add(table);
        table.id = tableList.size() - 1;
        table.parent = current;
        current = table;
    }

    // 当前符号表移动到父表
    private void moveUp() {
        current = current.parent;
    }

    // 向当前符号表添加符号
    void addSymbol(Symbol symbol) {
        if (!current.table.containsKey(symbol.name))
            current.table.put(symbol.name, symbol);
        else
            ErrorList.add(Error.ErrorType.IDENTIFIER_DUPLICATE, symbol.lineNumber);
    }
}

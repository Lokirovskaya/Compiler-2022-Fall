package symbol;

import lexer.Token;
import parser.Nonterminal;
import parser.TreeNode;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import static lexer.Token.TokenType.*;
import static parser.Nonterminal.NonterminalType.*;
import static symbol.Error.ErrorType.*;

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
    // 将多个 FuncFParam 加入此 function 的 params 中
    // 阻止上述的 Block 创建新符号表
    private Symbol.Function currentFunction;
    private boolean skipCreateTableOnce = false;

    // while 块的深度，为 0 说明不在 while 块中
    private int loopDepth = 0;

    private void runSyntaxTree(TreeNode _p) {
        if (_p instanceof Nonterminal) {
            Nonterminal p = (Nonterminal) _p;
            switch (p.type) {
                // 建符号表
                case _BLOCK_:
                    if (skipCreateTableOnce)
                        createTable();
                    skipCreateTableOnce = false;
                    break;
                case _FUNCTION_DEFINE_:
                case _MAIN_FUNCTION_DEFINE_:
                    currentFunction = TableUtil.readFunctionDefine(p);
                    skipCreateTableOnce = true;
                    addSymbol(currentFunction);
                    createTable();
                    if (!currentFunction.isVoid) {
                        TableUtil.checkFinalReturnOfIntFunction(p);
                    }
                    break;
                case _VAR_DEFINE_:
                case _CONST_DEFINE_:
                    Symbol.Var var = TableUtil.readVarDefine(p);
                    addSymbol(var);
                    break;
                case _FUNCTION_DEFINE_PARAM_:
                    assert currentFunction != null;
                    Symbol.Var param = TableUtil.readVarDefine(p);
                    currentFunction.params.add(param);
                    addSymbol(param);
                    break;

                // 错误处理
                case _STATEMENT_:
                    TreeNode firstChild = p.children.get(0);
                    if (firstChild.isType(PRINTF)) {
                        TableUtil.checkFormatString(p);
                    }
                    else if (firstChild.isType(RETURN)) {
                        if (currentFunction != null) {
                            TableUtil.checkReturnOfFunction(p, currentFunction.isVoid);
                        }
                    }
                    // 赋值语句
                    else if (firstChild.isType(_LEFT_VALUE_)) {
                        TableUtil.checkLeftValueConst(p, current);
                    }
                    else if (firstChild.isType(WHILE)) {
                        loopDepth++;
                    }
                    else if (firstChild.isType(BREAK) || firstChild.isType(CONTINUE)) {
                        if (loopDepth == 0) ErrorList.add(ILLEGAL_BREAK_CONTINUE, ((Token) firstChild).lineNumber);
                    }
                    break;
                case _LEFT_VALUE_:
                    TableUtil.checkUndefineVar(p, current);
                    break;
                case _UNARY_EXPRESSION_:
                    // function call
                    if (p.children.get(0).isType(IDENTIFIER)) {
                        TableUtil.checkFunctionCall(p, root, current);
                    }
                    break;
            }

            for (TreeNode next : p.children) {
                runSyntaxTree(next);
            }

            // exit
            if (p.isType(_BLOCK_)) {
                moveUp();
            }
            else if (p.isType(_FUNCTION_DEFINE_) || p.isType(_MAIN_FUNCTION_DEFINE_)) {
                currentFunction = null;
            }
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
            ErrorList.add(IDENTIFIER_DUPLICATE, symbol.lineNumber);
    }
}

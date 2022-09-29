package symbol;

import error.Error;
import error.ErrorList;
import lexer.Token;
import parser.Nonterminal;
import parser.TreeNode;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import static lexer.Token.TokenType.*;
import static parser.Nonterminal.NonterminalType.*;

public class TableBuilder {
    // 链式前向星
    private final List<Table> tableList = new ArrayList<>();
    private int currentIndex = -1;
    private final TreeNode syntaxTreeRoot;

    public TableBuilder(TreeNode syntaxTreeRoot) {
        this.syntaxTreeRoot = syntaxTreeRoot;
    }

    public List<Table> build() {
        createTable(); // root 表
        runSyntaxTree(syntaxTreeRoot);
        return tableList;
    }

    public void output(String filename) throws IOException {
        ResultOutput.output(filename, tableList);
    }

    // FuncDef → FuncType Ident '(' [FuncFParams] ')' Block
    // 请将多个 FuncFParam 加入此 function 的 params 中
    // 当它不为 null 时，阻止一次 Block 创建新符号表
    private Symbol.Function currentFunction = null;

    private void runSyntaxTree(TreeNode _p) {
        if (_p instanceof Nonterminal) {
            Nonterminal p = (Nonterminal) _p;
            switch (p.type) {
                case _BLOCK_:
                    if (currentFunction == null)
                        createTable();
                    currentFunction = null;
                    break;
                case _FUNCTION_DEFINE_:
                case _MAIN_FUNCTION_DEFINE_:
                    currentFunction = TableUtil.readFunctionDefine(p);
                    addSymbol(currentFunction);
                    createTable();
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
                case _STATEMENT_:
                    // printf
                    if (p.children.get(0) instanceof Token && ((Token) p.children.get(0)).type == PRINTF) {
                        TableUtil.checkFormatString(p);
                    }
                    break;
            }

            for (TreeNode next : p.children) {
                runSyntaxTree(next);
            }

            // exit
            if (p.type == _BLOCK_) moveUp();
        }

        else if (_p instanceof Token) {
            Token p = (Token) _p;
        }
    }

    // 新建符号表，作为当前表的子节点，然后将指针指向它
    private void createTable() {
        Table table = new Table();
        tableList.add(table);
        table.parentIndex = currentIndex;
        currentIndex = tableList.size() -1;
    }

    // 当前符号表移动到父表
    private void moveUp() {
        currentIndex = tableList.get(currentIndex).parentIndex;
    }

    // 向当前符号表添加符号
    void addSymbol(Symbol symbol) {
        if (!tableList.get(currentIndex).table.containsKey(symbol.name))
            tableList.get(currentIndex).table.put(symbol.name, symbol);
        else
            ErrorList.add(Error.ErrorType.IDENTIFIER_DUPLICATE, symbol.lineNumber);
    }
}

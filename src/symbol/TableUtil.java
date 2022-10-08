package symbol;

import error.ErrorList;
import lexer.Token;
import parser.Nonterminal;
import parser.TreeNode;

import java.util.List;

import static lexer.Token.TokenType.*;
import static parser.Nonterminal.NonterminalType.*;
import static error.Error.ErrorType.*;

// 提供一些建符号表和错误处理的实用程序类
public class TableUtil {
    // 向当前符号表添加符号
    static void addSymbol(Symbol symbol, Table currentTable) {
        if (!currentTable.table.containsKey(symbol.name))
            currentTable.table.put(symbol.name, symbol);
        else
            ErrorList.add(IDENTIFIER_DUPLICATE, symbol.lineNumber);
    }

    // 寻找名字对应的的符号
    public static Symbol findSymbol(String name, Table currentTable) {
        Table t = currentTable;
        while (t != null) {
            if (t.table.containsKey(name)) return t.table.get(name);
            t = t.parent;
        }
        return null;
    }

    // VarDef → Ident { '[' ConstExp ']' }
    // ConstDef → Ident { '[' ConstExp ']' } '=' ConstInitVal
    // FuncFParam → BType Ident ['[' ']' { '[' ConstExp ']' }]
    static Symbol.Var readVarDefine(Nonterminal varDefine) {
        assert varDefine.isType(_VAR_DEFINE_) || varDefine.isType(_CONST_DEFINE_) || varDefine.isType(_FUNCTION_DEFINE_PARAM_);
        List<TreeNode> nodes = varDefine.children;
        Symbol.Var var = new Symbol.Var();

        int identStart;
        if (varDefine.isType(_VAR_DEFINE_) || varDefine.isType(_CONST_DEFINE_))
            identStart = 0;
        else identStart = 1;

        var.name = ((Token) nodes.get(identStart)).value;
        var.lineNumber = ((Token) nodes.get(identStart)).lineNumber;
        var.isConst = (varDefine.isType(_CONST_DEFINE_));

        // 一直读到结束，有几个终结符 '['
        int bracketCount = 0;
        for (int i = identStart; i < nodes.size(); i++) {
            TreeNode p = nodes.get(i);
            if (p.isType(LEFT_BRACKET)) bracketCount++;
        }
        var.dimension = bracketCount;

        return var;
    }

    // FuncDef → FuncType Ident '(' [FuncFParams] ')' Block
    // MainFuncDef → 'int' 'main' '(' ')' Block
    static Symbol.Function readFunctionDefine(Nonterminal funcDef) {
        assert funcDef.isType(_FUNCTION_DEFINE_) || funcDef.isType(_MAIN_FUNCTION_DEFINE_);
        List<TreeNode> nodes = funcDef.children;
        Symbol.Function function = new Symbol.Function();

        if (funcDef.isType(_FUNCTION_DEFINE_)) {
            Nonterminal funcType = (Nonterminal) nodes.get(0);
            function.isVoid = funcType.children.get(0).isType(VOID);
            function.name = ((Token) nodes.get(1)).value;
            function.lineNumber = ((Token) nodes.get(1)).lineNumber;
        }
        else {
            function.isVoid = false;
            function.name = "main";
            function.lineNumber = ((Token) nodes.get(0)).lineNumber;
        }
        return function;
    }

}

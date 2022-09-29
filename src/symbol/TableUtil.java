package symbol;

import lexer.Token;
import parser.Nonterminal;
import parser.TreeNode;

import java.util.List;

import static symbol.Error.ErrorType.*;
import static lexer.Token.TokenType.*;
import static parser.Nonterminal.NonterminalType.*;

// 提供一些建符号表和错误处理的实用程序类
class TableUtil {
    // VarDef → Ident { '[' ConstExp ']' }
    // ConstDef → Ident { '[' ConstExp ']' } '=' ConstInitVal
    // FuncFParam → BType Ident ['[' ']' { '[' ConstExp ']' }]
    static Symbol.Var readVarDefine(Nonterminal varDefine) {
        assert varDefine.type == _VAR_DEFINE_ || varDefine.type == _CONST_DEFINE_ || varDefine.type == _FUNCTION_DEFINE_PARAM_;
        List<TreeNode> nodes = varDefine.children;
        Symbol.Var var = new Symbol.Var();

        int identStart;
        if (varDefine.type == _VAR_DEFINE_ || varDefine.type == _CONST_DEFINE_)
            identStart = 0;
        else identStart = 1;

        var.name = ((Token) nodes.get(identStart)).value;
        var.lineNumber = ((Token) nodes.get(identStart)).lineNumber;
        var.isConst = (varDefine.type == _CONST_DEFINE_);

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
        assert funcDef.type == _FUNCTION_DEFINE_ || funcDef.type == _MAIN_FUNCTION_DEFINE_;
        List<TreeNode> nodes = funcDef.children;
        Symbol.Function function = new Symbol.Function();

        if (funcDef.type == _FUNCTION_DEFINE_) {
            Nonterminal funcType = (Nonterminal) nodes.get(0);
            function.isVoid = (((Token) funcType.children.get(0)).type == VOID);
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

    static Symbol findSymbol(String name, Table currentTable) {
        Table t = currentTable;
        while (t != null) {
            if (t.table.containsKey(name)) return t.table.get(name);
            t = t.parent;
        }
        return null;
    }

    // LVal → Ident {'[' Exp ']'}
    static void checkUndefineVar(Nonterminal identUse, Table currentTable) {
        assert identUse.type == _LEFT_VALUE_;
        Token identifier = (Token) identUse.children.get(0);
        Symbol symbol = findSymbol(identifier.value, currentTable);
        if (symbol == null || symbol instanceof Symbol.Function) {
            ErrorList.add(IDENTIFIER_UNDEFINE, identifier.lineNumber);
        }
    }

    // UnaryExp → Ident '(' [FuncRParams] ')' | ...
    // 这里不检查参数类型是否匹配
    static void checkFunctionCall(Nonterminal functionCall, Table rootTable) {
        assert functionCall.type == _UNARY_EXPRESSION_;
        Token identifier = (Token) functionCall.children.get(0);

        Symbol symbol = findSymbol(identifier.value, rootTable);
        if (symbol == null || symbol instanceof Symbol.Var) {
            ErrorList.add(IDENTIFIER_UNDEFINE, identifier.lineNumber);
            return;
        }

        // FuncRParams → Exp { ',' Exp }
        int paramCount = 0;
        if (functionCall.children.get(2) instanceof Nonterminal) {
            Nonterminal paramListNode = (Nonterminal) functionCall.children.get(2);
            for (TreeNode t : paramListNode.children) {
                if (t.isType(_EXPRESSION_)) {
                    paramCount++;
                }
            }
        }

        if (paramCount != ((Symbol.Function) symbol).params.size()) {
            ErrorList.add(PARAM_COUNT_UNMATCH, identifier.lineNumber);
        }
    }

    // int 类型的函数，最后一条语句必须是 return Exp;
    static void checkReturnOfIntFunction(Nonterminal funcDef) {
        // FuncDef → FuncType Ident '(' [FuncFParams] ')' Block
        // MainFuncDef → 'int' 'main' '(' ')' Block
        assert funcDef.type == _FUNCTION_DEFINE_ || funcDef.type == _MAIN_FUNCTION_DEFINE_;
        Nonterminal block = (Nonterminal) funcDef.children.get(funcDef.children.size() - 1);
        // Block → '{' { BlockItem } '}'
        // BlockItem → Decl | Stmt
        TreeNode lastItem = block.children.get(block.children.size() - 2);
        int rightBraceLineNumber = ((Token) block.children.get(block.children.size() - 1)).lineNumber;
        Runnable error = () -> ErrorList.add(MISSING_RETURN, rightBraceLineNumber);

        if (!lastItem.isType(_BLOCK_ITEM_)) error.run(); // empty block, lastItem == '}'
        else {
            Nonterminal statement = (Nonterminal) ((Nonterminal) lastItem).children.get(0);
            if (!statement.isType(_STATEMENT_)) error.run(); // 最后一句是 declare
            else {
                if (!statement.children.get(0).isType(RETURN)) error.run(); // 不是 return 语句
                else if (!statement.children.get(1).isType(_EXPRESSION_)) error.run(); // return 空
            }
        }
    }

    // 符号表无关的错误检查 //

    // Stmt → 'printf' '(' FormatString { ',' Exp } ')' ';'
    static void checkFormatString(Nonterminal statement) {
        assert statement.type == _STATEMENT_;
        List<TreeNode> nodes = statement.children;
        String format = ((Token) nodes.get(2)).value;
        int formatStringLineNumber = ((Token) nodes.get(2)).lineNumber;
        int formatCharCount = 0;
        // 合法普通字符中，不包括 " # $ % & '
        // 跳过起始和结尾的 "
        for (int i = 1; i < format.length() - 1; i++) {
            char c = format.charAt(i);
            if (c == '%') {
                if (format.charAt(i + 1) == 'd') {
                    formatCharCount++;
                }
                else {
                    ErrorList.add(ILLEGAL_CHAR, formatStringLineNumber);
                    return;
                }
            }
            else if (c == '\\') {
                if (format.charAt(i + 1) != 'n') {
                    ErrorList.add(ILLEGAL_CHAR, formatStringLineNumber);
                    return;
                }
            }
            else if (!(c == 32 || c == 33 || 40 <= c && c <= 126)) {
                ErrorList.add(ILLEGAL_CHAR, formatStringLineNumber);
                return;
            }
        }

        int printfLineNumber = ((Token) nodes.get(0)).lineNumber;
        int formatParamCount = 0;
        int i = 3;
        while (((Token) nodes.get(i)).type == COMMA) {
            assert ((Nonterminal) nodes.get(i + 1)).type == _EXPRESSION_;
            formatParamCount++;
            i += 2;
        }

        if (formatCharCount != formatParamCount) {
            ErrorList.add(FORMAT_PARAM_COUNT_UNMATCH, printfLineNumber);
        }
    }
}

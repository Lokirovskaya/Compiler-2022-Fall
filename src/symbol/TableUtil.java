package symbol;

import lexer.Token;
import parser.Nonterminal;
import parser.TreeNode;

import java.util.ArrayList;
import java.util.List;

import static lexer.Token.TokenType.*;
import static parser.Nonterminal.NonterminalType.*;
import static symbol.Error.ErrorType.*;

// 提供一些建符号表和错误处理的实用程序类
class TableUtil {
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

    private static Symbol findSymbol(String name, Table currentTable) {
        Table t = currentTable;
        while (t != null) {
            if (t.table.containsKey(name)) return t.table.get(name);
            t = t.parent;
        }
        return null;
    }

    // LVal → Ident {'[' Exp ']'}
    static void checkUndefineVar(Nonterminal identUse, Table currentTable) {
        assert identUse.isType(_LEFT_VALUE_);
        Token identifier = (Token) identUse.children.get(0);
        Symbol symbol = findSymbol(identifier.value, currentTable);
        if (symbol == null || symbol instanceof Symbol.Function) {
            ErrorList.add(IDENTIFIER_UNDEFINE, identifier.lineNumber);
        }
    }

    // UnaryExp → Ident '(' [FuncRParams] ')' | ...
    static void checkFunctionCall(Nonterminal functionCall, Table rootTable, Table currentTable) {
        assert functionCall.isType(_UNARY_EXPRESSION_);
        Token identifier = (Token) functionCall.children.get(0);

        // 函数是否定义
        Symbol function = findSymbol(identifier.value, rootTable);
        if (function == null || function instanceof Symbol.Var) {
            ErrorList.add(IDENTIFIER_UNDEFINE, identifier.lineNumber);
            return;
        }

        // FuncRParams → Exp { ',' Exp }
        List<Nonterminal> paramList = new ArrayList<>();
        if (functionCall.children.get(2).isType(_FUNCTION_CALL_PARAM_LIST_)) {
            Nonterminal paramListNode = (Nonterminal) functionCall.children.get(2);
            for (TreeNode t : paramListNode.children) {
                if (t.isType(_EXPRESSION_)) {
                    paramList.add((Nonterminal) t);
                }
            }
        }

        // 函数参数数目是否匹配
        if (paramList.size() != ((Symbol.Function) function).params.size()) {
            ErrorList.add(PARAM_COUNT_UNMATCH, identifier.lineNumber);
            return;
        }
        // 函数参数类型是否匹配
        for (int i = 0, paramListSize = paramList.size(); i < paramListSize; i++) {
            TreeNode param = paramList.get(i);
            int rightValueDimension = getRightValueDimension(param, currentTable);
            if (rightValueDimension == -1) {
                ErrorList.add(PARAM_TYPE_UNMATCH, identifier.lineNumber);
            }
            else if (rightValueDimension >= 0) {
                int declareDimension = ((Symbol.Function) function).params.get(i).dimension;
                if (rightValueDimension != declareDimension) {
                    ErrorList.add(PARAM_TYPE_UNMATCH, identifier.lineNumber);
                }
            }
        }
    }

    // 前序遍历找到第一个 identifier 或 int const，再去查表，获得维数
    // 返回 -1 表明右值不合法（比如调用 void 函数，取地址超限等），-2 表示标识符未定义等异常
    private static int getRightValueDimension(TreeNode exp, Table currentTable) {
        assert exp.isType(_EXPRESSION_);
        List<TreeNode> stack = new ArrayList<>();
        stack.add(exp);
        while (!stack.isEmpty()) {
            TreeNode _t = stack.remove(stack.size() - 1);
            if (_t instanceof Token) continue;
            Nonterminal t = (Nonterminal) _t;
            // UnaryExp → Ident '(' [FuncRParams] ')' | ...
            // LVal → Ident {'[' Exp ']'}
            // Number → IntConst
            if (t.isType(_UNARY_EXPRESSION_)) {
                TreeNode identifier = t.children.get(0);
                if (identifier.isType(IDENTIFIER)) {
                    Symbol function = findSymbol(((Token) identifier).value, currentTable);
                    if (function == null || function instanceof Symbol.Var)
                        return -2;
                    if (((Symbol.Function) function).isVoid)
                        return -1;
                    return 0;
                }
            }
            else if (t.isType(_LEFT_VALUE_)) {
                Token identifier = (Token) t.children.get(0);
                int leftBracketCount = 0;
                for (TreeNode node : t.children) {
                    if (node.isType(LEFT_BRACKET)) leftBracketCount++;
                }
                Symbol var = findSymbol(identifier.value, currentTable);
                if (var == null || var instanceof Symbol.Function)
                    return -2;
                int declareDimension = ((Symbol.Var) var).dimension;
                if (leftBracketCount > declareDimension)
                    return -1;
                return declareDimension - leftBracketCount;
            }
            else if (t.isType(_NUMBER_)) {
                return 0;
            }

            for (int i = t.children.size() - 1; i >= 0; i--) {
                stack.add(t.children.get(i));
            }
        }
        return -2;
    }

    static void checkLeftValueConst(Nonterminal statement, Table currentTable) {
        assert statement.isType(_STATEMENT_);
        Nonterminal leftValue = (Nonterminal) statement.children.get(0);
        Token identifier = (Token) leftValue.children.get(0);
        Symbol var = findSymbol(identifier.value, currentTable);
        if (var == null || var instanceof Symbol.Function) return;
        if (((Symbol.Var) var).isConst) ErrorList.add(CHANGE_CONST, identifier.lineNumber);
    }

    static void checkReturnOfFunction(Nonterminal statement, boolean isVoid) {
        assert statement.isType(_STATEMENT_);
        // 'return' [Exp] ';'
        boolean hasExpression = statement.children.get(1).isType(_EXPRESSION_);
        boolean error = isVoid && hasExpression /* || !isVoid && !hasExpression */;
        int returnLineNumber = ((Token) statement.children.get(0)).lineNumber;
        if (error) ErrorList.add(RETURN_EXPRESSION_WHEN_VOID, returnLineNumber);
    }

    // 如果是 int 类型的函数，最后一条语句必须是 return Exp;
    static void checkFinalReturnOfIntFunction(Nonterminal funcDef) {
        // FuncDef → FuncType Ident '(' [FuncFParams] ')' Block
        // MainFuncDef → 'int' 'main' '(' ')' Block
        assert funcDef.isType(_FUNCTION_DEFINE_) || funcDef.isType(_MAIN_FUNCTION_DEFINE_);
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

    // Stmt → 'printf' '(' FormatString { ',' Exp } ')' ';'
    static void checkFormatString(Nonterminal statement) {
        assert statement.isType(_STATEMENT_);
        List<TreeNode> nodes = statement.children;
        String format = ((Token) nodes.get(2)).value;
        // 合法普通字符中，不包括 " # $ % & '
        int formatStringLineNumber = ((Token) nodes.get(2)).lineNumber;
        Runnable charError = () -> ErrorList.add(ILLEGAL_CHAR, formatStringLineNumber);
        for (int i = 1; i < format.length() - 1; i++) { // 跳过起始和结尾的 "
            char c = format.charAt(i);
            if (c == '%') {
                if (format.charAt(i + 1) != 'd') {
                    charError.run();
                    break;
                }
            }
            else if (c == '\\') {
                if (format.charAt(i + 1) != 'n') {
                    charError.run();
                    break;
                }
            }
            else if (!(c == 32 || c == 33 || (40 <= c && c <= 126))) {
                charError.run();
                break;
            }
        }

        int formatCharCount = 0;
        int formatParamCount = 0;
        int printfLineNumber = ((Token) nodes.get(0)).lineNumber;
        for (int i = 1; i < format.length() - 1; i++) {
            if (format.charAt(i) == '%' && format.charAt(i + 1) == 'd') formatCharCount++;
        }
        for (TreeNode t : nodes) {
            if (t.isType(COMMA)) formatParamCount++;
        }
        if (formatCharCount != formatParamCount) {
            ErrorList.add(FORMAT_PARAM_COUNT_UNMATCH, printfLineNumber);
        }
    }
}

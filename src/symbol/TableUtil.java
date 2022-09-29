package symbol;

import error.ErrorList;
import lexer.Token;
import parser.Nonterminal;
import parser.TreeNode;

import java.util.List;

import static error.Error.ErrorType.*;
import static lexer.Token.TokenType.*;
import static parser.Nonterminal.NonterminalType.*;

class TableUtil {
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
            if (p instanceof Token && ((Token) p).type == LEFT_BRACKET) bracketCount++;
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
}

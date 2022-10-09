package intercode;

import intercode.Quaternion.OperatorType;
import lexer.Token;
import parser.Nonterminal;
import parser.TreeNode;
import symbol.Symbol;

import java.util.Map;

import static intercode.Operand.*;
import static lexer.Token.TokenType.*;
import static parser.Nonterminal.NonterminalType.*;

public class Generator {
    private final InterCode inter = new InterCode();
    private final TreeNode syntaxTreeRoot;
    private final Map<Token, Symbol> identSymbolMap;
    private int regCount = 1;
    private static final VirtualReg zeroReg = new VirtualReg(0);

    public Generator(TreeNode syntaxTreeRoot, Map<Token, Symbol> identSymbolMap) {
        this.syntaxTreeRoot = syntaxTreeRoot;
        this.identSymbolMap = identSymbolMap;
    }

    public InterCode generate() {
        COMPILE_UNIT(syntaxTreeRoot);
        return inter;
    }

    // 用于存储表达式的结果
    private VirtualReg newReg() {
        return new VirtualReg(regCount++);
    }

    private VirtualReg getVarReg(Token ident) {
        assert ident.isType(IDENTIFIER);
        // 请保证传入的标识符对应变量或常量
        Symbol.Var var = (Symbol.Var) identSymbolMap.get(ident);
        if (var.reg == null) {
            var.reg = newReg();
            var.reg.name = var.name;
            var.reg.declareConst = var.isConst;
            var.reg.isAddr = var.dimension > 0;
        }
        return var.reg;
    }

    private Symbol.Function getFunc(Token ident) {
        assert ident.isType(IDENTIFIER);
        // 请保证传入的标识符对应函数
        return (Symbol.Function) identSymbolMap.get(ident);
    }

    private void COMPILE_UNIT(TreeNode _p) {
        // 跳过许多不必要的节点，从指定入口开始翻译
        if (_p instanceof Nonterminal) {
            Nonterminal p = (Nonterminal) _p;
            switch (p.type) {
                case _VAR_DEFINE_:
                case _CONST_DEFINE_:
                    VAR_DEFINE(p);
                    return;
                case _FUNCTION_DEFINE_:
                case _MAIN_FUNCTION_DEFINE_:
                    FUNCTION_DEFINE(p);
                    return;
            }
            for (TreeNode next : p.children) {
                COMPILE_UNIT(next);
            }
        }
    }

    private void VAR_DEFINE(Nonterminal def) {
        assert def.isType(_VAR_DEFINE_) || def.isType(_CONST_DEFINE_);
        TreeNode initVal = def.child(def.children.size() - 1);
        if (initVal.isType(_VAR_INIT_VALUE_) || initVal.isType(_CONST_INIT_VALUE_)) {
            Token ident = (Token) def.child(0);
            VirtualReg reg = getVarReg(ident);
            VAR_INIT_VALUE((Nonterminal) initVal, reg);
        }
    }

    // target 是继承属性，被赋值的寄存器
    private void VAR_INIT_VALUE(Nonterminal init, VirtualReg target) {
        assert init.isType(_VAR_INIT_VALUE_) || init.isType(_CONST_INIT_VALUE_);
        if (init.child(0).isType(_EXPRESSION_) || init.child(0).isType(_CONST_EXPRESSION_)) {
            VirtualReg expAns = newReg();
            EXPRESSION((Nonterminal) init.child(0), expAns);
            inter.addQuater(OperatorType.SET, target, expAns, null, null);
        }
    }

    private void FUNCTION_DEFINE(Nonterminal def) {
        assert def.isType(_FUNCTION_DEFINE_) || def.isType(_MAIN_FUNCTION_DEFINE_);
        if (def.isType(_MAIN_FUNCTION_DEFINE_)) {
            inter.addQuater(OperatorType.FUNC, null, null, null, new Label("main"));
        }
        else {
            Symbol.Function func = getFunc((Token) def.child(1));
            inter.addQuater(OperatorType.FUNC, null, null, null, new Label(func.name));
            // todo: params
        }
        BLOCK(def.child(def.children.size() - 1));
    }

    private void BLOCK(TreeNode _p) {
        // 跳过许多不必要的节点，从指定入口开始翻译
        if (_p instanceof Nonterminal) {
            Nonterminal p = (Nonterminal) _p;
            switch (p.type) {
                case _VAR_DEFINE_:
                case _CONST_DEFINE_:
                    VAR_DEFINE(p);
                    return;
                case _STATEMENT_:
                    STATEMENT(p);
                    return;
            }
            for (TreeNode next : p.children) {
                BLOCK(next);
            }
        }
    }

    private void STATEMENT(Nonterminal stmt) {
        assert stmt.isType(_STATEMENT_);
        // LVal '=' Exp ';' |  LVal '=' 'getint''('')'';'
        if (stmt.child(0).isType(_LEFT_VALUE_)) {
            // todo: array
            VirtualReg leftValue = getVarReg((Token) ((Nonterminal) stmt.child(0)).child(0));
            if (stmt.child(2).isType(_EXPRESSION_)) {
                VirtualReg expAns = newReg();
                EXPRESSION((Nonterminal) stmt.child(2), expAns);
                inter.addQuater(OperatorType.SET, leftValue, expAns, null, null);
            }
            else if (stmt.child(2).isType(GETINT)) {
                inter.addQuater(OperatorType.GETINT, leftValue, null, null, null);
            }
        }
        // [Exp] ';'
        else if (stmt.child(0).isType(_EXPRESSION_)) {
            EXPRESSION((Nonterminal) stmt.child(0), newReg());
        }
        // Block
        else if (stmt.child(0).isType(_BLOCK_)) {
            BLOCK(stmt.child(0));
        }
        // 'return' [Exp] ';'
        else if (stmt.child(0).isType(RETURN)) {
            if (stmt.child(1).isType(_EXPRESSION_)) {
                VirtualReg expAns = newReg();
                EXPRESSION((Nonterminal) stmt.child(1), expAns);
                inter.addQuater(OperatorType.RETURN, null, expAns, null, null);
            }
            else inter.addQuater(OperatorType.RETURN_VOID, null, null, null, null);
        }
    }


    // ans 是综合属性，表达式所得值的寄存器
    private void EXPRESSION(Nonterminal exp, VirtualReg ans) {
        assert exp.isType(_EXPRESSION_) || exp.isType(_CONST_EXPRESSION_);
        ADD_EXPRESSION((Nonterminal) exp.child(0), ans);
    }

    private void ADD_EXPRESSION(Nonterminal exp, VirtualReg ans) {
        assert exp.isType(_ADD_EXPRESSION_);
        if (exp.child(0).isType(_MULTIPLY_EXPRESSION_))
            MULTIPLY_EXPRESSION((Nonterminal) exp.child(0), ans);
        else {
            OperatorType op = exp.child(1).isType(PLUS) ? OperatorType.ADD : OperatorType.SUB;
            VirtualReg addAns = newReg(), multAns = newReg();
            ADD_EXPRESSION((Nonterminal) exp.child(0), addAns);
            MULTIPLY_EXPRESSION((Nonterminal) exp.child(2), multAns);
            inter.addQuater(op, ans, addAns, multAns, null);
        }
    }

    private void MULTIPLY_EXPRESSION(Nonterminal exp, VirtualReg ans) {
        assert exp.isType(_MULTIPLY_EXPRESSION_);
        if (exp.child(0).isType(_UNARY_EXPRESSION_))
            UNARY_EXPRESSION((Nonterminal) exp.child(0), ans);
        else {
            OperatorType op;
            if (exp.child(1).isType(MULTIPLY)) op = OperatorType.MULT;
            else if (exp.child(1).isType(DIVIDE)) op = OperatorType.DIV;
            else op = OperatorType.MOD;
            VirtualReg multAns = newReg(), unaryAns = newReg();
            MULTIPLY_EXPRESSION((Nonterminal) exp.child(0), multAns);
            UNARY_EXPRESSION((Nonterminal) exp.child(2), unaryAns);
            inter.addQuater(op, ans, multAns, unaryAns, null);
        }
    }

    private void UNARY_EXPRESSION(Nonterminal exp, VirtualReg ans) {
        assert exp.isType(_UNARY_EXPRESSION_);
        if (exp.child(0).isType(_PRIMARY_EXPRESSION_))
            PRIMARY_EXPRESSION((Nonterminal) exp.child(0), ans);
        else if (exp.child(0).isType(IDENTIFIER)) { // func call
            Token ident = (Token) exp.child(0);
            Symbol.Function func = getFunc(ident);
            // todo: params
            inter.addQuater(OperatorType.CALL, null, null, null, new Label(func.name));
            if (!func.isVoid)
                inter.addQuater(OperatorType.LOAD_RETURN, ans, null, null, null);
        }
        // UnaryExp → UnaryOp UnaryExp, UnaryOp → '+' | '−' | '!'
        else {
            Nonterminal unaryOp = (Nonterminal) exp.child(0);
            if (unaryOp.child(0).isType(PLUS)) { // do nothing
                UNARY_EXPRESSION((Nonterminal) exp.child(1), ans);
            }
            else if (unaryOp.child(0).isType(MINUS)) { // negate
                VirtualReg x = newReg();
                UNARY_EXPRESSION((Nonterminal) exp.child(1), x);
                inter.addQuater(OperatorType.SUB, ans, zeroReg, x, null);
            }
            else {
                // todo
            }
        }
    }

    private void PRIMARY_EXPRESSION(Nonterminal exp, VirtualReg ans) {
        assert exp.isType(_PRIMARY_EXPRESSION_);
        if (exp.child(0).isType(LEFT_PAREN))
            EXPRESSION((Nonterminal) exp.child(1), newReg());
        else if (exp.child(0).isType(_LEFT_VALUE_))
            LEFT_VALUE_EXPRESSION((Nonterminal) exp.child(0), ans);
        else if (exp.child(0).isType(_NUMBER_))
            NUMBER((Nonterminal) exp.child(0), ans);
    }

    private void LEFT_VALUE_EXPRESSION(Nonterminal exp, VirtualReg ans) {
        assert exp.isType(_LEFT_VALUE_);
        Token ident = (Token) exp.child(0);
        // todo: array
        inter.addQuater(OperatorType.SET, ans, getVarReg(ident), null, null);
    }

    private void NUMBER(Nonterminal exp, VirtualReg ans) {
        assert exp.isType(_NUMBER_);
        Token number = (Token) exp.child(0);
        int inst = Integer.parseInt(number.value);
        inter.addQuater(OperatorType.SET, ans, new InstNumber(inst), null, null);
    }
}

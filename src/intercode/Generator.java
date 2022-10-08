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
        runSyntaxTree(syntaxTreeRoot);
        return inter;
    }

    // 请只在发生运算的时候调用，因为当前 regID 已作为运算的 target
    private VirtualReg newReg() {
        return new VirtualReg(regCount++);
    }

    private VirtualReg getVarReg(Token ident) {
        assert ident.isType(IDENTIFIER);
        // 请保证传入的标识符对应变量或常量
        Symbol.Var var = (Symbol.Var) identSymbolMap.get(ident);
        VirtualReg reg = newReg();
        if (var.reg == null) {
            var.reg = reg;
            reg.var = var;
        }
        return var.reg;
    }

    private void runSyntaxTree(TreeNode _p) {
        if (_p instanceof Nonterminal) {
            Nonterminal p = ((Nonterminal) _p);
            switch (p.type) {
                case _EXPRESSION_:
                case _CONST_EXPRESSION_:
                    EXPRESSION(p, newReg());
                    return;
            }
            for (TreeNode next : p.children) {
                runSyntaxTree(next);
            }
        }
    }

    private void EXPRESSION(Nonterminal exp, VirtualReg reg) {
        ADD_EXPRESSION((Nonterminal) exp.children.get(0), reg);
    }

    private void ADD_EXPRESSION(Nonterminal exp, VirtualReg reg) {
        if (exp.children.get(0).isType(_MULTIPLY_EXPRESSION_))
            MULTIPLY_EXPRESSION((Nonterminal) exp.children.get(0), reg);
        else {
            OperatorType op = exp.children.get(1).isType(PLUS) ? OperatorType.ADD : OperatorType.SUB;
            VirtualReg x1 = newReg(), x2 = newReg();
            ADD_EXPRESSION((Nonterminal) exp.children.get(0), x1);
            MULTIPLY_EXPRESSION((Nonterminal) exp.children.get(2), x2);
            inter.addQuater(new Quaternion(op, reg, x1, x2, null));
        }
    }

    private void MULTIPLY_EXPRESSION(Nonterminal exp, VirtualReg reg) {
        if (exp.children.get(0).isType(_UNARY_EXPRESSION_))
            UNARY_EXPRESSION((Nonterminal) exp.children.get(0), reg);
        else {
            OperatorType op;
            if (exp.children.get(1).isType(MULTIPLY)) op = OperatorType.MULT;
            else if (exp.children.get(1).isType(DIVIDE)) op = OperatorType.DIV;
            else op = OperatorType.MOD;
            VirtualReg x1 = newReg(), x2 = newReg();
            MULTIPLY_EXPRESSION((Nonterminal) exp.children.get(0), x1);
            UNARY_EXPRESSION((Nonterminal) exp.children.get(2), x2);
            inter.addQuater(new Quaternion(op, reg, x1, x2, null));
        }
    }

    private void UNARY_EXPRESSION(Nonterminal exp, VirtualReg reg) {
        if (exp.children.get(0).isType(_PRIMARY_EXPRESSION_))
            PRIMARY_EXPRESSION((Nonterminal) exp.children.get(0), reg);
            // function call, UnaryExp → Ident '(' [FuncRParams] ')'
        else if (exp.children.get(0).isType(IDENTIFIER)) {
            // todo
        }
        // UnaryExp → UnaryOp UnaryExp, UnaryOp → '+' | '−' | '!'
        else {
            Nonterminal unaryOp = (Nonterminal) exp.children.get(0);
            if (unaryOp.children.get(0).isType(PLUS)) { // do nothing
                UNARY_EXPRESSION((Nonterminal) exp.children.get(1), reg);
            }
            else if (unaryOp.children.get(0).isType(MINUS)) { // negate
                VirtualReg x = newReg();
                UNARY_EXPRESSION((Nonterminal) exp.children.get(1), x);
                inter.addQuater(new Quaternion(OperatorType.SUB, reg, zeroReg, x, null));
            }
            else {
                // todo
            }
        }
    }

    private void PRIMARY_EXPRESSION(Nonterminal exp, VirtualReg reg) {
        if (exp.children.get(0).isType(LEFT_PAREN))
            EXPRESSION((Nonterminal) exp.children.get(1), newReg());
        else if (exp.children.get(0).isType(_LEFT_VALUE_))
            LEFT_VALUE_EXPRESSION((Nonterminal) exp.children.get(0), reg);
        else if (exp.children.get(0).isType(_NUMBER_))
            NUMBER((Nonterminal) exp.children.get(0), reg);
    }

    private void LEFT_VALUE_EXPRESSION(Nonterminal exp, VirtualReg reg) {
        Token ident = (Token) exp.children.get(0);
        // todo: array
        inter.addQuater(new Quaternion(OperatorType.SET, reg, getVarReg(ident), null, null));
    }

    private void NUMBER(Nonterminal exp, VirtualReg reg) {
        Token number = (Token) exp.children.get(0);
        int inst = Integer.parseInt(number.value);
        inter.addQuater(new Quaternion(OperatorType.SET, reg, new InstNumber(inst), null, null));
    }
}

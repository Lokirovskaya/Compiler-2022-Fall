package intercode;

import intercode.Quaternion.OperatorType;
import lexer.Token;
import parser.Nonterminal;
import parser.TreeNode;
import symbol.Symbol;
import util.Pair;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Stack;
import java.util.stream.Collectors;

import static intercode.Operand.*;
import static lexer.Token.TokenType.*;
import static parser.Nonterminal.NonterminalType.*;

public class Generator {
    private final InterCode inter = new InterCode();
    private final TreeNode syntaxTreeRoot;
    private final Map<Token, Symbol> identSymbolMap;
    private static final VirtualReg returnReg = new VirtualReg(0, "RET");
    private int regIdx = 1;
    private int labelIdx = 1;
    private final Stack<Pair<Label, Label>> whileLabelsList = new Stack<>();

    public Generator(TreeNode syntaxTreeRoot, Map<Token, Symbol> identSymbolMap) {
        this.syntaxTreeRoot = syntaxTreeRoot;
        this.identSymbolMap = identSymbolMap;
    }

    public InterCode generate() {
        COMPILE_UNIT(syntaxTreeRoot);
        return inter;
    }

    private Symbol.Var getVar(Token varIdent) {
        assert varIdent.isType(IDENTIFIER);
        return (Symbol.Var) identSymbolMap.get(varIdent);
    }

    private Symbol.Function getFunc(Token funcIdent) {
        assert funcIdent.isType(IDENTIFIER);
        return (Symbol.Function) identSymbolMap.get(funcIdent);
    }

    private VirtualReg newReg() {
        return new VirtualReg(regIdx++);
    }

    private Label newLabel() {
        return new Label("label_" + labelIdx++);
    }

    private void newQuater(Quaternion.OperatorType op, VirtualReg target, Operand x1, Operand x2, Label label) {
        inter.addLast(new Quaternion(op, target, x1, x2, label));
    }

    // 获得变量对应的寄存器
    // 如果未对变量分配寄存器，则分配一个，并设置寄存器的各属性
    // 返回的寄存器可能指向地址
    private VirtualReg getVarReg(Token varIdent) {
        assert varIdent.isType(IDENTIFIER);
        Symbol.Var var = getVar(varIdent);
        if (var.reg == null) {
            var.reg = newReg();
            var.reg.name = var.name + '_' + var.selfTable.id;
            var.reg.declareConst = var.isConst;
            var.reg.isAddr = var.isArray();
        }
        return var.reg;
    }

    // 获取多维数组调用的偏移量，若访问 a[x][y]，请传入 offset = {x,y}
    private Operand getLinearOffset(Symbol.Var array, List<VirtualReg> offset) {
        assert array.isArray();
        if (offset.size() == 0) return new InstNumber(0);
        if (array.dimension == 1) return offset.get(0);
        else {
            // ans = x * a.sizeOfDim1 + y
            VirtualReg multAns = newReg();
            newQuater(OperatorType.MULT, multAns, offset.get(0), array.sizeOfDim1, null);
            if (offset.size() == 1) return multAns; // 不完全取地址
            else {
                VirtualReg ans = newReg();
                newQuater(OperatorType.ADD, ans, multAns, offset.get(1), null);
                return ans;
            }
        }
    }

    // Generating begin! //

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
        Token ident = (Token) def.child(0);
        Symbol.Var var = getVar(ident);
        if (var.isArray()) {
            // Ident '[' ConstExp ']' '[' ConstExp ']'
            VirtualReg arrayReg = getVarReg(ident);
            if (var.dimension == 1) {
                var.sizeOfDim1 = EXPRESSION((Nonterminal) def.child(2));
                newQuater(OperatorType.ALLOC, arrayReg, var.sizeOfDim1, null, null);
            }
            else if (var.dimension == 2) {
                var.sizeOfDim1 = EXPRESSION((Nonterminal) def.child(5));
                var.sizeOfDim2 = EXPRESSION((Nonterminal) def.child(2));
                VirtualReg fullSize = newReg();
                newQuater(OperatorType.MULT, fullSize, var.sizeOfDim1, var.sizeOfDim2, null);
                newQuater(OperatorType.ALLOC, arrayReg, fullSize, null, null);
            }
        }
        // if def has an init value
        TreeNode initVal = def.child(def.children.size() - 1);
        if (initVal.isType(_VAR_INIT_VALUE_) || initVal.isType(_CONST_INIT_VALUE_)) {
            VAR_INIT_VALUE((Nonterminal) initVal, getVarReg(ident));
        }
    }

    // target 是继承属性，被赋值的寄存器
    private void VAR_INIT_VALUE(Nonterminal init, VirtualReg target) {
        assert init.isType(_VAR_INIT_VALUE_) || init.isType(_CONST_INIT_VALUE_);
        if (!target.isAddr) {
            VirtualReg expAns = EXPRESSION((Nonterminal) init.child(0));
            newQuater(OperatorType.SET, target, expAns, null, null);
        }
        else {
            List<VirtualReg> initExpList = new ArrayList<>();
            findChildExpressions(init, initExpList);
            for (int i = 0; i < initExpList.size(); i++) {
                newQuater(OperatorType.SET_ARRAY, target, new InstNumber(i), initExpList.get(i), null);
            }
        }
    }

    // InitVal → Exp | '{' [ InitVal { ',' InitVal } ] '}'
    // 前序遍历找出此递归文法的所有子 Exp，它是数组的从左到右的初值
    private void findChildExpressions(Nonterminal p, List<VirtualReg> ans) {
        assert p.isType(_VAR_INIT_VALUE_) || p.isType(_CONST_INIT_VALUE_);
        for (TreeNode child : p.children) {
            if (child.isType(_EXPRESSION_)) {
                ans.add(EXPRESSION((Nonterminal) child));
                return;
            }
            else if (child.isType(_VAR_INIT_VALUE_) || child.isType(_CONST_INIT_VALUE_)) {
                findChildExpressions((Nonterminal) child, ans);
            }
        }
    }

    private void FUNCTION_DEFINE(Nonterminal def) {
        assert def.isType(_FUNCTION_DEFINE_) || def.isType(_MAIN_FUNCTION_DEFINE_);
        if (def.isType(_MAIN_FUNCTION_DEFINE_)) {
            newQuater(OperatorType.FUNC, null, null, null, new Label("main"));
        }
        else {
            // FuncDef → FuncType Ident '(' [FuncFParams] ')' Block
            Symbol.Function func = getFunc((Token) def.child(1));
            newQuater(OperatorType.FUNC, null, null, null, new Label(func.name));
            if (def.child(3).isType(_FUNCTION_DEFINE_PARAM_LIST_)) {
                for (TreeNode p : ((Nonterminal) def.child(3)).children) {
                    if (p.isType(_FUNCTION_DEFINE_PARAM_))
                        FUNCTION_DEFINE_PARAM((Nonterminal) p);
                }
            }
        }
        BLOCK(def.child(def.children.size() - 1));
    }

    private void FUNCTION_DEFINE_PARAM(Nonterminal paramDef) {
        assert paramDef.isType(_FUNCTION_DEFINE_PARAM_);
        Token ident = (Token) paramDef.child(1);
        Symbol.Var param = getVar(ident);
        VirtualReg paramReg = getVarReg(ident);
        if (param.isArray()) {
            // BType Ident '[' ']' '[' ConstExp ']'
            if (param.dimension == 1) {
                newQuater(OperatorType.PARAM, null, paramReg, null, null);
            }
            else if (param.dimension == 2) {
                param.sizeOfDim1 = EXPRESSION((Nonterminal) paramDef.child(5));
                newQuater(OperatorType.PARAM, null, paramReg, null, null);
            }
        }
        else {
            newQuater(OperatorType.PARAM, null, paramReg, null, null);
        }
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
            VirtualReg expAns;
            if (stmt.child(2).isType(_EXPRESSION_)) {
                expAns = EXPRESSION((Nonterminal) stmt.child(2));
            }
            else { // getint exp
                expAns = newReg();
                newQuater(OperatorType.GETINT, expAns, null, null, null);
            }
            Nonterminal leftValue = (Nonterminal) stmt.child(0);
            Token ident = (Token) leftValue.child(0);
            Symbol.Var var = getVar(ident);
            if (var.isArray()) {
                List<VirtualReg> offset = leftValue.children.stream()
                        .filter(p -> p.isType(_EXPRESSION_))
                        .map(e -> EXPRESSION((Nonterminal) e))
                        .collect(Collectors.toList());
                Operand linearOffset = getLinearOffset(var, offset);
                newQuater(OperatorType.SET_ARRAY, getVarReg(ident), linearOffset, expAns, null);
            }
            else {
                newQuater(OperatorType.SET, getVarReg(ident), expAns, null, null);
            }
        }
        // [Exp] ';'
        else if (stmt.child(0).isType(_EXPRESSION_)) {
            EXPRESSION((Nonterminal) stmt.child(0));
        }
        // Block
        else if (stmt.child(0).isType(_BLOCK_)) {
            BLOCK(stmt.child(0));
        }
        // 'return' [Exp] ';'
        else if (stmt.child(0).isType(RETURN)) {
            if (stmt.child(1).isType(_EXPRESSION_)) {
                VirtualReg expAns = EXPRESSION((Nonterminal) stmt.child(1));
                newQuater(OperatorType.SET, returnReg, expAns, null, null);
            }
            newQuater(OperatorType.RETURN, null, null, null, null);
        }
        // 'if' '(' Cond ')' Stmt [ 'else' Stmt ]
        else if (stmt.child(0).isType(IF)) {
            boolean hasElse = stmt.children.size() > 5;
            if (!hasElse) {
                Label trueLabel = newLabel(), falseLabel = newLabel();
                CONDITION((Nonterminal) stmt.child(2), trueLabel, falseLabel);
                newQuater(OperatorType.LABEL, null, null, null, trueLabel);
                STATEMENT((Nonterminal) stmt.child(4));
                newQuater(OperatorType.LABEL, null, null, null, falseLabel);
            }
            else {
                Label trueLabel = newLabel(), falseLabel = newLabel(), endLabel = newLabel();
                CONDITION((Nonterminal) stmt.child(2), trueLabel, falseLabel);
                newQuater(OperatorType.LABEL, null, null, null, trueLabel);
                STATEMENT((Nonterminal) stmt.child(4));
                newQuater(OperatorType.GOTO, null, null, null, endLabel);
                newQuater(OperatorType.LABEL, null, null, null, falseLabel);
                STATEMENT((Nonterminal) stmt.child(6));
                newQuater(OperatorType.LABEL, null, null, null, endLabel);
            }
        }
        // 'while' '(' Cond ')' Stmt
        else if (stmt.child(0).isType(WHILE)) {
            Label startLabel = newLabel(), trueLabel = newLabel(), falseLabel = newLabel();
            whileLabelsList.push(new Pair<>(startLabel, falseLabel));
            newQuater(OperatorType.LABEL, null, null, null, startLabel);
            CONDITION((Nonterminal) stmt.child(2), trueLabel, falseLabel);
            newQuater(OperatorType.LABEL, null, null, null, trueLabel);
            STATEMENT((Nonterminal) stmt.child(4));
            newQuater(OperatorType.GOTO, null, null, null, startLabel);
            newQuater(OperatorType.LABEL, null, null, null, falseLabel);
            whileLabelsList.pop();
        }
        // 'break' ';'
        else if (stmt.child(0).isType(BREAK)) {
            newQuater(OperatorType.GOTO, null, null, null, whileLabelsList.peek().second);
        }
        // 'continue' ';'
        else if (stmt.child(0).isType(CONTINUE)) {
            newQuater(OperatorType.GOTO, null, null, null, whileLabelsList.peek().first);
        }
        // 'printf' '(' FormatString { ',' Exp } ')' ';'
        else if (stmt.child(0).isType(PRINTF)) {
            String formatTokenValue = ((Token) stmt.child(2)).value;
            String format = formatTokenValue.substring(1, formatTokenValue.length() - 1); // 跳过前后双引号
            List<VirtualReg> expList = stmt.children.stream()
                    .filter(p -> p.isType(_EXPRESSION_))
                    .map(e -> EXPRESSION((Nonterminal) e))
                    .collect(Collectors.toList());
            int expIdx = 0;
            StringBuilder buffer = new StringBuilder();
            Runnable printAndClearBuffer = () -> {
                // 输出 buffer（如果非空），然后清空 buffer；如果 buffer 长度为 1，简化为输出单字符
                if (buffer.length() > 0) {
                    String str = buffer.toString();
                    if (str.length() > 1) {
                        VirtualReg strReg = newReg();
                        strReg.isAddr = true;
                        newQuater(OperatorType.ALLOC_STR, strReg, null, null, new Label(str));
                        newQuater(OperatorType.PRINT_STR, null, strReg, null, null);
                    }
                    else {
                        newQuater(OperatorType.PRINT_CHAR, null, new InstNumber(str.charAt(0)), null, null);
                    }
                    buffer.delete(0, buffer.length());
                }
            };
            for (int i = 0; i < format.length(); i++) {
                if (format.charAt(i) == '%') {
                    // 输出 buffer，再输出一个数字
                    assert format.charAt(i + 1) == 'd';
                    i++;
                    printAndClearBuffer.run();
                    newQuater(OperatorType.PRINT_INT, null, expList.get(expIdx++), null, null);
                }
                else buffer.append(format.charAt(i));
            }
            printAndClearBuffer.run();
        }
    }


    private VirtualReg EXPRESSION(Nonterminal exp) {
        assert exp.isType(_EXPRESSION_) || exp.isType(_CONST_EXPRESSION_);
        return ADD_EXPRESSION((Nonterminal) exp.child(0));
    }

    private VirtualReg ADD_EXPRESSION(Nonterminal exp) {
        assert exp.isType(_ADD_EXPRESSION_);
        if (exp.child(0).isType(_MULTIPLY_EXPRESSION_))
            return MULTIPLY_EXPRESSION((Nonterminal) exp.child(0));
        else {
            OperatorType op = exp.child(1).isType(PLUS) ? OperatorType.ADD : OperatorType.SUB;
            VirtualReg ans = newReg();
            newQuater(op, ans,
                    ADD_EXPRESSION((Nonterminal) exp.child(0)),
                    MULTIPLY_EXPRESSION((Nonterminal) exp.child(2)), null);
            return ans;
        }
    }

    private VirtualReg MULTIPLY_EXPRESSION(Nonterminal exp) {
        assert exp.isType(_MULTIPLY_EXPRESSION_);
        if (exp.child(0).isType(_UNARY_EXPRESSION_)) {
            return UNARY_EXPRESSION((Nonterminal) exp.child(0));
        }
        else {
            OperatorType op;
            if (exp.child(1).isType(MULTIPLY)) op = OperatorType.MULT;
            else if (exp.child(1).isType(DIVIDE)) op = OperatorType.DIV;
            else op = OperatorType.MOD;
            VirtualReg ans = newReg();
            newQuater(op, ans,
                    MULTIPLY_EXPRESSION((Nonterminal) exp.child(0)),
                    UNARY_EXPRESSION((Nonterminal) exp.child(2)), null);
            return ans;
        }
    }

    private VirtualReg UNARY_EXPRESSION(Nonterminal exp) {
        assert exp.isType(_UNARY_EXPRESSION_);
        if (exp.child(0).isType(_PRIMARY_EXPRESSION_)) {
            return PRIMARY_EXPRESSION((Nonterminal) exp.child(0));
        }
        // function call, UnaryExp → Ident '(' [FuncRParams] ')'
        else if (exp.child(0).isType(IDENTIFIER)) {
            Token ident = (Token) exp.child(0);
            Symbol.Function func = getFunc(ident);
            if (exp.child(2).isType(_FUNCTION_CALL_PARAM_LIST_)) {
                for (TreeNode p : ((Nonterminal) exp.child(2)).children) {
                    if (p.isType(_EXPRESSION_)) {
                        VirtualReg paramAns = EXPRESSION((Nonterminal) p);
                        newQuater(OperatorType.PUSH, null, paramAns, null, null);
                    }
                }
            }
            newQuater(OperatorType.CALL, null, null, null, new Label(func.name));
            if (!func.isVoid) return returnReg;
            else return null;
        }
        // UnaryExp → UnaryOp UnaryExp, UnaryOp → '+' | '−' | '!'
        else {
            Nonterminal unaryOp = (Nonterminal) exp.child(0);
            if (unaryOp.child(0).isType(PLUS)) { // do nothing
                return UNARY_EXPRESSION((Nonterminal) exp.child(1));
            }
            else if (unaryOp.child(0).isType(MINUS)) { // negate
                VirtualReg ans = newReg();
                newQuater(OperatorType.SUB, ans,
                        new InstNumber(0), UNARY_EXPRESSION((Nonterminal) exp.child(1)), null);
                return ans;
            }
            else {
                VirtualReg ans = newReg();
                newQuater(OperatorType.NOT, ans,
                        UNARY_EXPRESSION((Nonterminal) exp.child(1)), null, null);
                return ans;
            }
        }
    }

    private VirtualReg PRIMARY_EXPRESSION(Nonterminal exp) {
        assert exp.isType(_PRIMARY_EXPRESSION_);
        if (exp.child(0).isType(LEFT_PAREN))
            return EXPRESSION((Nonterminal) exp.child(1));
        else if (exp.child(0).isType(_LEFT_VALUE_))
            return LEFT_VALUE_EXPRESSION((Nonterminal) exp.child(0));
        else
            return NUMBER((Nonterminal) exp.child(0));
    }

    private VirtualReg LEFT_VALUE_EXPRESSION(Nonterminal exp) {
        assert exp.isType(_LEFT_VALUE_);
        Token ident = (Token) exp.child(0);
        Symbol.Var var = getVar(ident);
        if (var.isArray()) {
            List<VirtualReg> offset = exp.children.stream()
                    .filter(p -> p.isType(_EXPRESSION_))
                    .map(e -> EXPRESSION((Nonterminal) e))
                    .collect(Collectors.toList());
            Operand linearOffset = getLinearOffset(var, offset);
            VirtualReg arrAns = newReg();
            // 完全取地址
            if (var.dimension == offset.size()) {
                newQuater(OperatorType.GET_ARRAY, arrAns, getVarReg(ident), linearOffset, null);
            }
            // 不完全取地址，返回仍是一个地址
            else {
                newQuater(OperatorType.ADD, arrAns, getVarReg(ident), linearOffset, null);
                arrAns.isAddr = true;
            }
            return arrAns;
        }
        else return getVarReg(ident);
    }

    private VirtualReg NUMBER(Nonterminal exp) {
        assert exp.isType(_NUMBER_);
        Token number = (Token) exp.child(0);
        int inst = Integer.parseInt(number.value);
        VirtualReg ans = newReg();
        newQuater(OperatorType.SET, ans, new InstNumber(inst), null, null);
        return ans;
    }

    private void CONDITION(Nonterminal cond, Label trueLabel, Label falseLabel) {
        assert cond.isType(_CONDITION_);
        LOGIC_OR_EXPRESSION((Nonterminal) cond.child(0), trueLabel, falseLabel);
    }

    private void LOGIC_OR_EXPRESSION(Nonterminal cond, Label trueLabel, Label falseLabel) {
        assert cond.isType(_LOGIC_OR_EXPRESSION_);
        if (cond.child(0).isType(_LOGIC_AND_EXPRESSION_)) {
            LOGIC_AND_EXPRESSION((Nonterminal) cond.child(0), trueLabel, falseLabel);
        }
        else {
            Label newFalseLabel = newLabel();
            LOGIC_OR_EXPRESSION((Nonterminal) cond.child(0), trueLabel, newFalseLabel);
            newQuater(OperatorType.LABEL, null, null, null, newFalseLabel);
            LOGIC_AND_EXPRESSION((Nonterminal) cond.child(2), trueLabel, falseLabel);

        }
    }

    private void LOGIC_AND_EXPRESSION(Nonterminal cond, Label trueLabel, Label falseLabel) {
        assert cond.isType(_LOGIC_AND_EXPRESSION_);
        VirtualReg condExpAns;
        if (cond.child(0).isType(_EQUAL_EXPRESSION_)) {
            condExpAns = EQUAL_EXPRESSION((Nonterminal) cond.child(0));
        }
        else {
            Label newTrueLabel = newLabel();
            LOGIC_AND_EXPRESSION((Nonterminal) cond.child(0), newTrueLabel, falseLabel);
            newQuater(OperatorType.LABEL, null, null, null, newTrueLabel);
            condExpAns = EQUAL_EXPRESSION((Nonterminal) cond.child(2));
        }
        newQuater(OperatorType.IF, null, condExpAns, null, trueLabel);
        newQuater(OperatorType.GOTO, null, null, null, falseLabel);
    }

    private VirtualReg EQUAL_EXPRESSION(Nonterminal cond) {
        assert cond.isType(_EQUAL_EXPRESSION_);
        if (cond.child(0).isType(_RELATION_EXPRESSION_)) {
            return RELATION_EXPRESSION((Nonterminal) cond.child(0));
        }
        else {
            OperatorType op = cond.child(1).isType(EQUAL) ? OperatorType.EQ : OperatorType.NOT_EQ;
            VirtualReg ans = newReg();
            newQuater(op, ans,
                    EQUAL_EXPRESSION((Nonterminal) cond.child(0)),
                    RELATION_EXPRESSION((Nonterminal) cond.child(2)), null);
            return ans;
        }
    }

    private VirtualReg RELATION_EXPRESSION(Nonterminal cond) {
        assert cond.isType(_RELATION_EXPRESSION_);
        if (cond.child(0).isType(_ADD_EXPRESSION_)) {
            return ADD_EXPRESSION((Nonterminal) cond.child(0));
        }
        else {
            OperatorType op;
            if (cond.child(1).isType(LESS)) op = OperatorType.LESS;
            else if (cond.child(1).isType(LESS_EQUAL)) op = OperatorType.LESS_EQ;
            else if (cond.child(1).isType(GREATER)) op = OperatorType.GREATER;
            else op = OperatorType.GREATER_EQ;
            VirtualReg ans = newReg();
            newQuater(op, ans,
                    RELATION_EXPRESSION((Nonterminal) cond.child(0)),
                    ADD_EXPRESSION((Nonterminal) cond.child(2)), null);
            return ans;
        }
    }
}

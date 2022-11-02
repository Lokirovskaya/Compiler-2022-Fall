package intercode;

import intercode.Quaternion.OperatorType;
import lexer.Token;
import parser.Nonterminal;
import parser.TreeNode;
import symbol.Symbol;
import util.Pair;

import java.util.*;
import java.util.stream.Collectors;

import static intercode.Operand.InstNumber;
import static intercode.Operand.VirtualReg;
import static lexer.Token.TokenType.*;
import static parser.Nonterminal.NonterminalType.*;

public class Generator {
    private final InterCode inter = new InterCode();
    private final TreeNode syntaxTreeRoot;
    private final Map<Token, Symbol> identSymbolMap;
    private int regIdx = 1;
    private int labelIdx = 1;
    private static final VirtualReg returnReg = new VirtualReg(0);
    private final Stack<Pair<Label, Label>> whileLabelsList = new Stack<>();

    public Generator(TreeNode syntaxTreeRoot, Map<Token, Symbol> identSymbolMap) {
        this.syntaxTreeRoot = syntaxTreeRoot;
        this.identSymbolMap = identSymbolMap;
    }

    public InterCode generate() {
        COMPILE_UNIT(syntaxTreeRoot);
        return inter;
    }

    static {
        returnReg.name = "RET";
        returnReg.realReg = 2;
        returnReg.isGlobal = true;
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

    private Symbol.Var getVar(Token varIdent) {
        assert varIdent.isType(IDENTIFIER);
        return (Symbol.Var) identSymbolMap.get(varIdent);
    }

    private Symbol.Function getFunc(Token funcIdent) {
        assert funcIdent.isType(IDENTIFIER);
        return (Symbol.Function) identSymbolMap.get(funcIdent);
    }

    // 获得变量对应的寄存器
    // 如果未对变量分配寄存器，则分配一个，并设置寄存器的各属性
    // 返回的寄存器可能指向地址
    private VirtualReg getVarReg(Token varIdent) {
        assert varIdent.isType(IDENTIFIER);
        Symbol.Var var = getVar(varIdent);
        assert !(var.isGlobal() && var.isArray());
        if (var.reg == null) {
            var.reg = newReg();
            var.reg.name = var.name;
            var.reg.tableID = var.selfTable.id;
            var.reg.isAddr = var.isArray();
            var.reg.isGlobal = (var.selfTable.id == 0);
        }
        return var.reg;
    }

    // 获取全局数组的 Tag，若没有，则分配一个
    private Label getVarTag(Token varIdent) {
        assert varIdent.isType(IDENTIFIER);
        Symbol.Var var = getVar(varIdent);
        assert var.isGlobal() && var.isArray();
        return new Label("arr_" + var.name);
    }

    // 获取多维数组调用的偏移量，若访问 a[x][y]，请传入 offset = {x,y}
    private Operand getLinearOffset(Symbol.Var array, List<Operand> offset) {
        assert array.isArray();
        if (offset.size() == 0) return new InstNumber(0);
        if (array.dimension == 1) return offset.get(0);
        else {
            // ans = x * a.sizeOfDim1 + y
            Operand multAns;
            if (offset.get(0) instanceof InstNumber) {
                multAns = new InstNumber(((InstNumber) offset.get(0)).number * array.sizeOfDim1.number);
            }
            else {
                multAns = newReg();
                newQuater(OperatorType.MULT, (VirtualReg) multAns, offset.get(0), array.sizeOfDim1, null);
            }

            if (offset.size() == 1) return multAns; // 不完全取地址

            if (multAns instanceof InstNumber && offset.get(1) instanceof InstNumber) {
                return new InstNumber(((InstNumber) multAns).number + ((InstNumber) offset.get(1)).number);
            }
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
        Quaternion alloc = null; // 数组声明若有初值，初值保存在 alloc 的 list 中
        if (var.isArray()) {
            // Ident '[' ConstExp ']' '[' ConstExp ']'
            InstNumber fullSize;
            if (var.dimension == 1) {
                var.sizeOfDim1 = (InstNumber) EXPRESSION((Nonterminal) def.child(2));
                fullSize = new InstNumber(var.sizeOfDim1.number);
            }
            else {
                var.sizeOfDim1 = (InstNumber) EXPRESSION((Nonterminal) def.child(5));
                var.sizeOfDim2 = (InstNumber) EXPRESSION((Nonterminal) def.child(2));
                fullSize = new InstNumber(var.sizeOfDim1.number * var.sizeOfDim2.number);
            }
            if (var.isGlobal()) {
                Label arrayTag = getVarTag(ident);
                alloc = new Quaternion(OperatorType.GLOBAL_ALLOC, null, fullSize, null, arrayTag);
            }
            else {
                VirtualReg arrayReg = getVarReg(ident);
                alloc = new Quaternion(OperatorType.ALLOC, arrayReg, fullSize, null, null);
            }
        }

        // 如果有初值
        TreeNode initVal = def.child(def.children.size() - 1);
        if (initVal.isType(_VAR_INIT_VALUE_) || initVal.isType(_CONST_INIT_VALUE_)) {
            if (!var.isArray()) {
                getVarReg(ident); // 为变量分配 vreg，如果是常量，这个 vreg 实际上会被丢弃
                Operand expAns = EXPRESSION((Nonterminal) ((Nonterminal) initVal).child(0));
                if (var.isConst) var.constVal = (InstNumber) expAns;
                else newQuater(OperatorType.SET, var.reg, expAns, null, null);
            }
            else {
                // 存放数组 vreg/tag 在 alloc 时分配过了；数组占用的栈空间，在 mips.Allocator 分配
                List<Operand> initExpList = new ArrayList<>();
                findChildExpressions(((Nonterminal) initVal), initExpList);
                assert alloc != null;
                alloc.list = initExpList;
                if (var.isConst) {
                    var.constArrayVal = initExpList.stream().map(e -> (InstNumber) e).collect(Collectors.toList());
                }
            }
        }
        // 没有初值的全局变量，应该要赋值为 0
        else {
            if (var.isGlobal() && !var.isArray()) {
                newQuater(OperatorType.SET, getVarReg(ident), new InstNumber(0), null, null);
            }
        }

        if (alloc != null) inter.addLast(alloc);
    }

    // InitVal → Exp | '{' [ InitVal { ',' InitVal } ] '}'
    // 前序遍历找出此递归文法的所有子 Exp，它是数组的从左到右的初值
    private void findChildExpressions(Nonterminal p, List<Operand> ans) {
        assert p.isType(_VAR_INIT_VALUE_) || p.isType(_CONST_INIT_VALUE_);
        for (TreeNode child : p.children) {
            if (child.isType(_EXPRESSION_) || child.isType(_CONST_EXPRESSION_)) {
                ans.add(EXPRESSION((Nonterminal) child));
                return;
            }
            else if (child.isType(_VAR_INIT_VALUE_) || child.isType(_CONST_INIT_VALUE_)) {
                findChildExpressions((Nonterminal) child, ans);
            }
        }
    }

    private boolean firstFunc = true;

    private void FUNCTION_DEFINE(Nonterminal def) {
        assert def.isType(_FUNCTION_DEFINE_) || def.isType(_MAIN_FUNCTION_DEFINE_);
        if (firstFunc) {
            newQuater(OperatorType.CALL, null, null, null, new Label("main"));
            newQuater(OperatorType.EXIT, null, null, null, null);
            firstFunc = false;
        }

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
                newQuater(OperatorType.PARAM, paramReg, null, null, null);
            }
            else if (param.dimension == 2) {
                param.sizeOfDim1 = (InstNumber) EXPRESSION((Nonterminal) paramDef.child(5));
                newQuater(OperatorType.PARAM, paramReg, null, null, null);
            }
        }
        else {
            newQuater(OperatorType.PARAM, paramReg, null, null, null);
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
            Operand expAns;
            if (stmt.child(2).isType(_EXPRESSION_)) {
                expAns = EXPRESSION((Nonterminal) stmt.child(2));
            }
            // getint exp
            else {
                expAns = newReg();
                newQuater(OperatorType.GETINT, (VirtualReg) expAns, null, null, null);
            }
            Nonterminal leftValue = (Nonterminal) stmt.child(0);
            Token ident = (Token) leftValue.child(0);
            Symbol.Var var = getVar(ident);
            if (var.isArray()) {
                List<Operand> offset = leftValue.children.stream()
                        .filter(p -> p.isType(_EXPRESSION_))
                        .map(e -> EXPRESSION((Nonterminal) e))
                        .collect(Collectors.toList());
                Operand linearOffset = getLinearOffset(var, offset);
                if (var.isGlobal())
                    newQuater(OperatorType.SET_GLOBAL_ARRAY, null, linearOffset, expAns, getVarTag(ident));
                else
                    newQuater(OperatorType.SET_ARRAY, getVarReg(ident), linearOffset, expAns, null);
            }
            else {
                newQuater(OperatorType.SET, getVarReg(ident), expAns, null, null);
            }
        }
        // [Exp] ';'
        else if (stmt.child(0).isType(_EXPRESSION_)) {
            EXPRESSION((Nonterminal) stmt.child(0)); // ans 被丢弃
        }
        // Block
        else if (stmt.child(0).isType(_BLOCK_)) {
            BLOCK(stmt.child(0));
        }
        // 'return' [Exp] ';'
        else if (stmt.child(0).isType(RETURN)) {
            if (stmt.child(1).isType(_EXPRESSION_)) {
                Operand expAns = EXPRESSION((Nonterminal) stmt.child(1));
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
            List<Operand> expAnsList = stmt.children.stream()
                    .filter(p -> p.isType(_EXPRESSION_))
                    .map(e -> EXPRESSION((Nonterminal) e))
                    .collect(Collectors.toList());
            int expIdx = 0;
            StringBuilder buffer = new StringBuilder();
            Runnable printAndClearBuffer = () -> {
                // 输出 buffer（如果非空），然后清空 buffer；如果 buffer 长度为 1，或 "\n"，简化为输出单字符
                if (buffer.length() > 0) {
                    String str = buffer.toString();
                    if (str.length() == 1)
                        newQuater(OperatorType.PRINT_CHAR, null, new InstNumber(str.charAt(0)), null, null);
                    else if (str.equals("\\n"))
                        newQuater(OperatorType.PRINT_CHAR, null, new InstNumber('\n'), null, null);
                    else
                        newQuater(OperatorType.PRINT_STR, null, null, null, new Label(str));
                    buffer.delete(0, buffer.length());
                }
            };
            String format = ((Token) stmt.child(2)).value;
            for (int i = 1; i < format.length() - 1; i++) {  // 跳过前后双引号
                if (format.charAt(i) == '%') {
                    // 输出 buffer，再输出一个数字
                    assert format.charAt(i + 1) == 'd';
                    i++;
                    printAndClearBuffer.run();
                    newQuater(OperatorType.PRINT_INT, null, expAnsList.get(expIdx++), null, null);
                }
                else buffer.append(format.charAt(i));
            }
            printAndClearBuffer.run();
        }
    }

    private Operand EXPRESSION(Nonterminal exp) {
        assert exp.isType(_EXPRESSION_) || exp.isType(_CONST_EXPRESSION_);
        Operand ans = ADD_EXPRESSION((Nonterminal) exp.child(0));
        if (exp.isType(_CONST_EXPRESSION_)) assert ans instanceof InstNumber;
        return ans;
    }

    private Operand ADD_EXPRESSION(Nonterminal exp) {
        assert exp.isType(_ADD_EXPRESSION_);
        if (exp.child(0).isType(_MULTIPLY_EXPRESSION_))
            return MULTIPLY_EXPRESSION((Nonterminal) exp.child(0));
        else {
            OperatorType op = exp.child(1).isType(PLUS) ? OperatorType.ADD : OperatorType.SUB;
            Operand addAns = ADD_EXPRESSION((Nonterminal) exp.child(0));
            Operand multAns = MULTIPLY_EXPRESSION((Nonterminal) exp.child(2));
            if (addAns instanceof InstNumber && multAns instanceof InstNumber) {
                int x1 = ((InstNumber) addAns).number;
                int x2 = ((InstNumber) multAns).number;
                return new InstNumber(op == OperatorType.ADD ? x1 + x2 : x1 - x2);
            }
            else {
                VirtualReg ans = newReg();
                newQuater(op, ans, addAns, multAns, null);
                return ans;
            }
        }
    }

    private Operand MULTIPLY_EXPRESSION(Nonterminal exp) {
        assert exp.isType(_MULTIPLY_EXPRESSION_);
        if (exp.child(0).isType(_UNARY_EXPRESSION_)) {
            return UNARY_EXPRESSION((Nonterminal) exp.child(0));
        }
        else {
            OperatorType op;
            if (exp.child(1).isType(MULTIPLY)) op = OperatorType.MULT;
            else if (exp.child(1).isType(DIVIDE)) op = OperatorType.DIV;
            else op = OperatorType.MOD;
            Operand multAns = MULTIPLY_EXPRESSION((Nonterminal) exp.child(0));
            Operand unaryAns = UNARY_EXPRESSION((Nonterminal) exp.child(2));
            if (multAns instanceof InstNumber && unaryAns instanceof InstNumber) {
                int x1 = ((InstNumber) multAns).number;
                int x2 = ((InstNumber) unaryAns).number;
                int y;
                if (op == OperatorType.MULT) y = x1 * x2;
                else if (op == OperatorType.DIV) y = x1 / x2;
                else y = x1 % x2;
                return new InstNumber(y);
            }
            else {
                VirtualReg ans = newReg();
                newQuater(op, ans, multAns, unaryAns, null);
                return ans;
            }
        }
    }

    private Operand UNARY_EXPRESSION(Nonterminal exp) {
        assert exp.isType(_UNARY_EXPRESSION_);
        if (exp.child(0).isType(_PRIMARY_EXPRESSION_)) {
            return PRIMARY_EXPRESSION((Nonterminal) exp.child(0));
        }
        // function call, UnaryExp → Ident '(' [FuncRParams] ')'
        else if (exp.child(0).isType(IDENTIFIER)) {
            Token ident = (Token) exp.child(0);
            Symbol.Function func = getFunc(ident);
            List<Operand> paramList = new ArrayList<>();
            if (exp.child(2).isType(_FUNCTION_CALL_PARAM_LIST_)) {
                for (TreeNode p : ((Nonterminal) exp.child(2)).children) {
                    if (p.isType(_EXPRESSION_)) {
                        Operand paramAns = EXPRESSION((Nonterminal) p);
                        paramList.add(paramAns);
                    }
                }
            }
            Quaternion call = new Quaternion(OperatorType.CALL, null, null, null, new Label(func.name));
            call.list = paramList;
            inter.addLast(call);
            if (!func.isVoid) {
                VirtualReg ans = newReg();
                newQuater(OperatorType.SET, ans, returnReg, null, null);
                return ans;
            }
            else return null;
        }
        // UnaryExp → UnaryOp UnaryExp, UnaryOp → '+' | '−' | '!'
        else {
            Nonterminal unaryOp = (Nonterminal) exp.child(0);
            Operand unaryAns = UNARY_EXPRESSION((Nonterminal) exp.child(1));
            if (unaryOp.child(0).isType(PLUS)) { // do nothing
                return unaryAns;
            }
            else if (unaryOp.child(0).isType(MINUS)) { // negate
                if (unaryAns instanceof InstNumber)
                    return new InstNumber(-((InstNumber) unaryAns).number);
                else {
                    VirtualReg ans = newReg();
                    newQuater(OperatorType.NEG, ans, unaryAns, null, null);
                    return ans;
                }
            }
            else {
                if (unaryAns instanceof InstNumber)
                    return new InstNumber(((InstNumber) unaryAns).number != 0 ? 0 : 1);
                else {
                    VirtualReg ans = newReg();
                    newQuater(OperatorType.NOT, ans, unaryAns, null, null);
                    return ans;
                }
            }
        }
    }

    private Operand PRIMARY_EXPRESSION(Nonterminal exp) {
        assert exp.isType(_PRIMARY_EXPRESSION_);
        if (exp.child(0).isType(LEFT_PAREN))
            return EXPRESSION((Nonterminal) exp.child(1));
        else if (exp.child(0).isType(_LEFT_VALUE_))
            return LEFT_VALUE_EXPRESSION((Nonterminal) exp.child(0));
        else
            return NUMBER((Nonterminal) exp.child(0));
    }

    private Operand LEFT_VALUE_EXPRESSION(Nonterminal exp) {
        assert exp.isType(_LEFT_VALUE_);
        Token ident = (Token) exp.child(0);
        Symbol.Var var = getVar(ident);
        if (!var.isArray()) {
            if (var.isConst) return var.constVal;
            else return getVarReg(ident);
        }
        else {
            // Ident '[' Exp ']' '[' Exp ']'
            List<Operand> offset = exp.children.stream()
                    .filter(p -> p.isType(_EXPRESSION_))
                    .map(e -> EXPRESSION((Nonterminal) e))
                    .collect(Collectors.toList());
            if (var.dimension == 1) {
                if (var.isConst && offset.get(0) instanceof InstNumber) {
                    return var.constArrayVal.get(((InstNumber) offset.get(0)).number);
                }
            }
            else if (var.dimension == 2) {
                if (var.isConst && offset.get(0) instanceof InstNumber && offset.get(1) instanceof InstNumber) {
                    int idx = ((InstNumber) offset.get(0)).number * var.sizeOfDim1.number + ((InstNumber) offset.get(1)).number;
                    return var.constArrayVal.get(idx);
                }
            }
            Operand linearOffset = getLinearOffset(var, offset);
            VirtualReg arrAns = newReg();
            // 完全取地址
            if (var.dimension == offset.size()) {
                if (var.isGlobal())
                    newQuater(OperatorType.GET_GLOBAL_ARRAY, arrAns, null, linearOffset, getVarTag(ident));
                else
                    newQuater(OperatorType.GET_ARRAY, arrAns, getVarReg(ident), linearOffset, null);
            }
            // 不完全取地址，返回仍是一个地址
            else {
                if (var.isGlobal())
                    newQuater(OperatorType.ADD_GLOBAL_ADDR, arrAns, null, linearOffset, getVarTag(ident));
                else
                    newQuater(OperatorType.ADD_ADDR, arrAns, getVarReg(ident), linearOffset, null);
                arrAns.isAddr = true;
            }
            return arrAns;
        }
    }

    private Operand NUMBER(Nonterminal exp) {
        assert exp.isType(_NUMBER_);
        Token number = (Token) exp.child(0);
        int inst = Integer.parseInt(number.value);
        return new InstNumber(inst);
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
        Operand condExpAns;
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

    private Operand EQUAL_EXPRESSION(Nonterminal cond) {
        assert cond.isType(_EQUAL_EXPRESSION_);
        if (cond.child(0).isType(_RELATION_EXPRESSION_)) {
            return RELATION_EXPRESSION((Nonterminal) cond.child(0));
        }
        else {
            OperatorType op = cond.child(1).isType(EQUAL) ? OperatorType.EQ : OperatorType.NOT_EQ;
            Operand eqAns = EQUAL_EXPRESSION((Nonterminal) cond.child(0));
            Operand relAns = RELATION_EXPRESSION((Nonterminal) cond.child(2));
            if (eqAns instanceof InstNumber && relAns instanceof InstNumber) {
                int x1 = ((InstNumber) eqAns).number;
                int x2 = ((InstNumber) relAns).number;
                int y;
                if (op == OperatorType.EQ) y = x1 == x2 ? 1 : 0;
                else y = x1 != x2 ? 1 : 0;
                return new InstNumber(y);
            }
            else {
                VirtualReg ans = newReg();
                newQuater(op, ans, eqAns, relAns, null);
                return ans;
            }
        }
    }

    private Operand RELATION_EXPRESSION(Nonterminal cond) {
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
            Operand relAns = RELATION_EXPRESSION((Nonterminal) cond.child(0));
            Operand addAns = ADD_EXPRESSION((Nonterminal) cond.child(2));
            if (relAns instanceof InstNumber && addAns instanceof InstNumber) {
                int x1 = ((InstNumber) relAns).number;
                int x2 = ((InstNumber) addAns).number;
                int y;
                if (op == OperatorType.LESS) y = x1 < x2 ? 1 : 0;
                else if (op == OperatorType.LESS_EQ) y = x1 <= x2 ? 1 : 0;
                else if (op == OperatorType.GREATER) y = x1 > x2 ? 1 : 0;
                else y = x1 >= x2 ? 1 : 0;
                return new InstNumber(y);
            }
            else {
                VirtualReg ans = newReg();
                newQuater(op, ans, relAns, addAns, null);
                return ans;
            }
        }
    }
}

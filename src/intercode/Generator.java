package intercode;

import intercode.Quaternion.OperatorType;
import lexer.Token;
import parser.Nonterminal;
import parser.TreeNode;
import symbol.Symbol;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.zip.ZipEntry;

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

    private Symbol.Var getVar(Token varIdent) {
        assert varIdent.isType(IDENTIFIER);
        return (Symbol.Var) identSymbolMap.get(varIdent);
    }

    private Symbol.Function getFunc(Token funcIdent) {
        assert funcIdent.isType(IDENTIFIER);
        return (Symbol.Function) identSymbolMap.get(funcIdent);
    }

    private VirtualReg newReg() {
        return new VirtualReg(regCount++);
    }

    private void newQuater(Quaternion.OperatorType op, VirtualReg target, Operand x1, Operand x2, Label label) {
        inter.addQuater(new Quaternion(op, target, x1, x2, label));
    }

    // 获得变量对应的寄存器
    // 如果未对变量分配寄存器，则分配一个，并设置寄存器的各属性
    // 返回的寄存器可能指向地址
    private VirtualReg getVarReg(Token varIdent) {
        assert varIdent.isType(IDENTIFIER);
        Symbol.Var var = getVar(varIdent);
        if (var.reg == null) {
            var.reg = newReg();
            var.reg.name = var.name;
            var.reg.declareConst = var.isConst;
            var.reg.isAddr = var.isArray();
        }
        return var.reg;
    }

    // 设置数组的 volume 字段，若定义 int a[x][y][z]，请传入 size = {y,z}
    private void setVolume(Symbol.Var array, List<VirtualReg> size) {
        assert array.isArray();
        assert size.size() == array.volume.length - 1;
        if (array.dimension > 1) {
            // volume[k-1] = volume[k] * size[k-1], volume[-2] = size[-1]
            array.volume[array.volume.length - 2] = size.get(size.size() - 1);
            for (int k = array.volume.length - 2; k >= 1; k--) {
                VirtualReg volume = newReg();
                newQuater(OperatorType.MULT, volume, array.volume[k], size.get(k - 1), null);
                array.volume[k - 1] = volume;
            }
        }
    }

    // 获取多维数组调用的偏移量，若访问 a[x][y][z]，请传入 offset = {x,y,z}
    private VirtualReg getLinearOffset(Symbol.Var array, List<VirtualReg> offset) {
        assert array.isArray();
        if (array.dimension == 1) return offset.get(0);
        else {
            // return sum(offset[i]*volume[i]) (offset.len <= volume.len, volume[-1] == 1)
            // 计算上述值的伪代码:
            // if offset.len == volume.len: ans = offset[offset.len-1]
            // else: ans = offset[offset.len-1] * volume[offset.len-1]
            // for i from (offset.len-2) to 0 : ans += offset[i] * volume[i]
            // return ans
            VirtualReg ans = newReg();
            VirtualReg multAns = newReg();
            if (offset.size() == array.volume.length)
                newQuater(OperatorType.SET, ans, offset.get(offset.size() - 1), null, null);
            else
                newQuater(OperatorType.MULT, ans, offset.get(offset.size() - 1), array.volume[offset.size() - 1], null);
            for (int i = offset.size() - 2; i >= 0; i--) {
                newQuater(OperatorType.MULT, multAns, offset.get(i), array.volume[i], null);
                newQuater(OperatorType.ADD, ans, ans, multAns, null);
            }
            return ans;
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
            List<VirtualReg> size = def.children.stream()
                    .filter(p -> p.isType(_CONST_EXPRESSION_))
                    .skip(1) // 最高维参数不重要
                    .map(e -> EXPRESSION((Nonterminal) e))
                    .collect(Collectors.toList());
            setVolume(var, size);
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
        if (init.child(0).isType(_EXPRESSION_) || init.child(0).isType(_CONST_EXPRESSION_)) {
            VirtualReg expAns = EXPRESSION((Nonterminal) init.child(0));
            newQuater(OperatorType.SET, target, expAns, null, null);
        }
    }

    private void FUNCTION_DEFINE(Nonterminal def) {
        assert def.isType(_FUNCTION_DEFINE_) || def.isType(_MAIN_FUNCTION_DEFINE_);
        if (def.isType(_MAIN_FUNCTION_DEFINE_)) {
            newQuater(OperatorType.FUNC, null, null, null, new Label("main"));
        }
        else {
            Symbol.Function func = getFunc((Token) def.child(1));
            newQuater(OperatorType.FUNC, null, null, null, new Label(func.name));
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
                VirtualReg linearOffset = getLinearOffset(var, offset);
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
                newQuater(OperatorType.RETURN, null, expAns, null, null);
            }
            else newQuater(OperatorType.RETURN_VOID, null, null, null, null);
        }
    }

    // ans 是综合属性，表达式所得值的寄存器
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
        // function call
        else if (exp.child(0).isType(IDENTIFIER)) {
            Token ident = (Token) exp.child(0);
            Symbol.Function func = getFunc(ident);
            // todo: params
            newQuater(OperatorType.CALL, null, null, null, new Label(func.name));
            if (!func.isVoid) {
                VirtualReg ans = newReg();
                newQuater(OperatorType.LOAD_RETURN, ans, null, null, null);
                return ans;
            }
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
                        zeroReg,
                        UNARY_EXPRESSION((Nonterminal) exp.child(1)), null);
                return ans;
            }
            else {
                // todo '!'
                return null;
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
        VirtualReg ans = newReg();
        Token ident = (Token) exp.child(0);
        Symbol.Var var = getVar(ident);
        if (var.isArray()) {
            List<VirtualReg> offset = exp.children.stream()
                    .filter(p -> p.isType(_EXPRESSION_))
                    .map(e -> EXPRESSION((Nonterminal) e))
                    .collect(Collectors.toList());
            VirtualReg linearOffset = getLinearOffset(var, offset);
            newQuater(OperatorType.GET_ARRAY, ans, getVarReg(ident), linearOffset, null);
        }
        else {
            newQuater(OperatorType.SET, ans, getVarReg(ident), null, null);
        }
        return ans;
    }

    private VirtualReg NUMBER(Nonterminal exp) {
        assert exp.isType(_NUMBER_);
        Token number = (Token) exp.child(0);
        int inst = Integer.parseInt(number.value);
        VirtualReg ans = newReg();
        newQuater(OperatorType.SET, ans, new InstNumber(inst), null, null);
        return ans;
    }
}

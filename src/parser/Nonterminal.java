package parser;

import java.util.ArrayList;
import java.util.List;

public class Nonterminal extends TreeNode {
    public NonterminalType type;
    public List<TreeNode> children = new ArrayList<>();

    public Nonterminal(NonterminalType type) {
        this.type = type;
    }

    @Override
    public boolean isType(NonterminalType t) {
        return this.type == t;
    }

    public enum NonterminalType {
        _COMPILE_UNIT_, _DECLARE_, _BASIC_TYPE_, _CONST_DECLARE_, _CONST_DEFINE_, _CONST_INIT_VALUE_,
        _VAR_DECLARE_, _VAR_DEFINE_, _VAR_INIT_VALUE_, _FUNCTION_DEFINE_, _MAIN_FUNCTION_DEFINE_,
        _FUNCTION_TYPE_, _FUNCTION_DEFINE_PARAM_LIST_, _FUNCTION_DEFINE_PARAM_, _BLOCK_, _BLOCK_ITEM_,
        _STATEMENT_, _EXPRESSION_, _CONDITION_, _LEFT_VALUE_, _PRIMARY_EXPRESSION_, _NUMBER_,
        _UNARY_EXPRESSION_, _UNARY_OPERATOR_, _FUNCTION_CALL_PARAM_LIST_, _MULTIPLY_EXPRESSION_,
        _ADD_EXPRESSION_, _RELATION_EXPRESSION_, _EQUAL_EXPRESSION_, _LOGIC_AND_EXPRESSION_,
        _LOGIC_OR_EXPRESSION_, _CONST_EXPRESSION_
    }
}

package intercode;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.List;

import static intercode.Quaternion.OperatorType.*;

class ResultOutput {
    public static void output(InterCode inter, String filename) throws IOException {
        StringBuilder sb = new StringBuilder();
        inter.forEach(node -> {
            Quaternion q = node.get(0);
            if (q.op == SET) sb.append(String.format("%s = %s\n", q.target, q.x1));
            else if (q.op == ADD) sb.append(String.format("%s = %s + %s\n", q.target, q.x1, q.x2));
            else if (q.op == SUB) sb.append(String.format("%s = %s - %s\n", q.target, q.x1, q.x2));
            else if (q.op == MULT) sb.append(String.format("%s = %s * %s\n", q.target, q.x1, q.x2));
            else if (q.op == DIV) sb.append(String.format("%s = %s / %s\n", q.target, q.x1, q.x2));
            else if (q.op == MOD) sb.append(String.format("%s = %s %% %s\n", q.target, q.x1, q.x2));
            else if (q.op == NEG) sb.append(String.format("%s = -%s\n", q.target, q.x1));
            else if (q.op == GET_ARRAY) sb.append(String.format("%s = %s[%s]\n", q.target, q.x1, q.x2));
            else if (q.op == SET_ARRAY) sb.append(String.format("%s[%s] = %s\n", q.target, q.x1, q.x2));
            else if (q.op == GET_GLOBAL_ARRAY) sb.append(String.format("%s = %s[%s]\n", q.target, q.label, q.x2));
            else if (q.op == SET_GLOBAL_ARRAY) sb.append(String.format("%s[%s] = %s\n", q.label, q.x1, q.x2));
            else if (q.op == ADD_ADDR) sb.append(String.format("%s = &%s[%s]\n", q.target, q.x1, q.x2));
            else if (q.op == ADD_GLOBAL_ADDR) sb.append(String.format("%s = &%s[%s]\n", q.target, q.label, q.x2));
            else if (q.op == IF) sb.append(String.format("if %s goto %s\n", q.x1, q.label));
            else if (q.op == IF_NOT) sb.append(String.format("if_not %s goto %s\n", q.x1, q.label));
            else if (q.op == IF_EQ) sb.append(String.format("if_cond %s == %s goto %s\n", q.x1, q.x2, q.label));
            else if (q.op == IF_NOT_EQ) sb.append(String.format("if_cond %s != %s goto %s\n", q.x1, q.x2, q.label));
            else if (q.op == IF_LESS) sb.append(String.format("if_cond %s < %s goto %s\n", q.x1, q.x2, q.label));
            else if (q.op == IF_LESS_EQ) sb.append(String.format("if_cond %s <= %s goto %s\n", q.x1, q.x2, q.label));
            else if (q.op == IF_GREATER) sb.append(String.format("if_cond %s > %s goto %s\n", q.x1, q.x2, q.label));
            else if (q.op == IF_GREATER_EQ) sb.append(String.format("if_cond %s >= %s goto %s\n", q.x1, q.x2, q.label));
            else if (q.op == NOT) sb.append(String.format("%s = !%s\n", q.target, q.x1));
            else if (q.op == EQ) sb.append(String.format("%s = %s == %s\n", q.target, q.x1, q.x2));
            else if (q.op == NOT_EQ) sb.append(String.format("%s = %s != %s\n", q.target, q.x1, q.x2));
            else if (q.op == LESS) sb.append(String.format("%s = %s < %s\n", q.target, q.x1, q.x2));
            else if (q.op == LESS_EQ) sb.append(String.format("%s = %s <= %s\n", q.target, q.x1, q.x2));
            else if (q.op == GREATER) sb.append(String.format("%s = %s > %s\n", q.target, q.x1, q.x2));
            else if (q.op == GREATER_EQ) sb.append(String.format("%s = %s >= %s\n", q.target, q.x1, q.x2));
            else if (q.op == GETINT) sb.append(String.format("%s = getint\n", q.target));
            else if (q.op == PRINT_INT) sb.append(String.format("printi %s\n", q.x1));
            else if (q.op == PRINT_STR) sb.append(String.format("prints %s\n", q.label));
            else if (q.op == PRINT_CHAR) sb.append(String.format("printc %s\n", q.x1));
            else if (q.op == FUNC) sb.append(String.format("\nfunc %s\n", q.label));
            else if (q.op == LABEL) sb.append(String.format("%s:\n", q.label));
            else if (q.op == GOTO) sb.append(String.format("goto %s\n", q.label));
            else if (q.op == RETURN) sb.append("return\n");
            else if (q.op == EXIT) sb.append("exit\n");
            else if (q.op == CALL) sb.append(String.format("call %s %s\n", q.label, operandListToString(q.list)));
            else if (q.op == PARAM) sb.append(String.format("param %s\n", q.target));
            else if (q.op == ALLOC) sb.append(String.format("%s = alloc %s %s\n", q.target, q.x1, operandListToString(q.list)));
            else if (q.op == GLOBAL_ALLOC) sb.append(String.format("%s = global_alloc %s %s\n", q.label, q.x1, operandListToString(q.list)));
            else if (q.op == STR_DECLARE) sb.append(String.format("str_%s = \"%s\"\n", q.x1, q.label));
        });
        Files.write(Paths.get(filename), sb.toString().trim().getBytes(StandardCharsets.UTF_8));
    }

    private static String operandListToString(List<Operand> list) {
        if (list == null) return "()";
        else return "(" + list.toString().substring(1, list.toString().length() - 1) + ")";
    }
}

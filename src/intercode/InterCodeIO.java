package intercode;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.List;

import static intercode.Quaternion.OperatorType.*;

public class InterCodeIO {
    public static InterCode input(String filename) {
        InterCode intercode = new InterCode();
        try {
            List<String> lines = Files.readAllLines(Paths.get(filename), StandardCharsets.UTF_8);
            for (String line : lines) {
                String[] tokens = line.split("\\s+");
                Quaternion quater = new Quaternion();
                Quaternion.OperatorType op = Quaternion.OperatorType.valueOf(tokens[0]);
                quater.op = op;
                if (op == ADD || op == SUB || op == MULT || op == DIV || op == MOD) {
                    quater.target = (Operand.VirtualReg) StringToOperand(tokens[1]);
                    quater.x1 = StringToOperand(tokens[2]);
                    quater.x2 = StringToOperand(tokens[3]);
                }
                else if (op == SET) {
                    quater.target = (Operand.VirtualReg) StringToOperand(tokens[1]);
                    quater.x1 = StringToOperand(tokens[2]);
                }
                intercode.addQuaternion(quater);
            }
        }
        catch (IOException e) {
            e.printStackTrace();
        }
        return intercode;
    }

    //
    public static void output(InterCode intercode, String filename) {
        StringBuilder sb = new StringBuilder();
        for (Quaternion q : intercode.list) {
            if (q.op == SET) sb.append(String.format("%s = %s\n", q.target, q.x1));
            else if (q.op == ADD) sb.append(String.format("%s = %s + %s\n", q.target, q.x1, q.x2));
            else if (q.op == SUB) sb.append(String.format("%s = %s - %s\n", q.target, q.x1, q.x2));
            else if (q.op == MULT) sb.append(String.format("%s = %s * %s\n", q.target, q.x1, q.x2));
            else if (q.op == DIV) sb.append(String.format("%s = %s / %s\n", q.target, q.x1, q.x2));
            else if (q.op == MOD) sb.append(String.format("%s = %s %% %s\n", q.target, q.x1, q.x2));
            else if (q.op == GET_ARRAY) sb.append(String.format("%s = %s[%s]\n", q.target, q.x1, q.x2));
            else if (q.op == SET_ARRAY) sb.append(String.format("%s[%s] = %s\n", q.target, q.x1, q.x2));
            else if (q.op == LOAD_ADDR) sb.append(String.format("%s = &%s\n", q.target, q.x1));
            else if (q.op == IF_EQ) sb.append(String.format("if %s == %s goto %s\n", q.x1, q.x2, q.label));
            else if (q.op == IF_NEQ) sb.append(String.format("if %s != %s goto %s\n", q.x1, q.x2, q.label));
            else if (q.op == IF_LESS) sb.append(String.format("if %s < %s goto %s\n", q.x1, q.x2, q.label));
            else if (q.op == IF_LESS_EQ) sb.append(String.format("if %s <= %s goto %s\n", q.x1, q.x2, q.label));
            else if (q.op == IF_GREATER) sb.append(String.format("if %s > %s goto %s\n", q.x1, q.x2, q.label));
            else if (q.op == IF_GREATER_EQ) sb.append(String.format("if %s >= %s goto %s\n", q.x1, q.x2, q.label));
            else if (q.op == GETINT) sb.append(String.format("%s = getint\n", q.target));
            else if (q.op == PRINT_INT) sb.append(String.format("printi %s\n", q.x1));
            else if (q.op == PRINT_STR) sb.append(String.format("prints %s\n", q.label));
            else if (q.op == PRINT_CHAR) sb.append(String.format("printc %s\n", q.x1));
            else if (q.op == FUNC) sb.append(String.format("func %s\n", q.label));
            else if (q.op == LABEL) sb.append(String.format("label %s\n", q.label));
            else if (q.op == GOTO) sb.append(String.format("goto %s\n", q.label));
            else if (q.op == RETURN) sb.append(String.format("return %s\n", q.x1));
            else if (q.op == LOAD_RETURN) sb.append(String.format("%s = @ret\n", q.target));
            else if (q.op == CALL) sb.append(String.format("call %s\n", q.label));
            else if (q.op == PUSH) sb.append(String.format("push %s\n", q.x1));
            else if (q.op == PARAM) sb.append(String.format("param %s\n", q.x1));
            else if (q.op == PARAM_ARRAY) sb.append(String.format("param[] %s\n", q.x1));
            else if (q.op == ALLOC) sb.append(String.format("%s = alloc %s\n", q.target, q.x1));
        }
        try {
            Files.write(Paths.get(filename), sb.toString().getBytes(StandardCharsets.UTF_8));
        }
        catch (IOException e) {
            e.printStackTrace();
        }
    }

    private static Operand StringToOperand(String str) {
        if (str.startsWith("@"))
            return new Operand.VirtualReg(Integer.parseInt(str.substring(1)));
        else return new Operand.InstNumber(Integer.parseInt(str));
    }
}

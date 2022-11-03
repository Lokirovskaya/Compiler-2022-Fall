package optimizer;

import intercode.InterCode;
import intercode.Label;
import intercode.Operand.InstNumber;
import intercode.Quaternion;

import static intercode.Quaternion.OperatorType.*;

public class MergePrint {
    static void run(InterCode inter) {
        StringBuilder buff = new StringBuilder();
        inter.forEach(p -> {
            String nowStr = getPrintString(p.get());
            String nextStr = p.get(1) == null ? null : getPrintString(p.get(1));
            if (nowStr != null && nextStr != null) {
                buff.append(nowStr);
                while (nextStr != null) {
                    buff.append(nextStr);
                    p.delete(1);
                    nextStr = p.get(1) == null ? null : getPrintString(p.get(1));
                }
                p.get().op = PRINT_STR;
                p.get().x1 = null;
                p.get().label = new Label(buff.toString());
                buff.delete(0, buff.length());
            }
        });
    }

    private static String getPrintString(Quaternion print) {
        if (print.op == PRINT_INT) {
            if (print.x1 instanceof InstNumber)
                return String.valueOf(((InstNumber) print.x1).number);
            else return null;
        }
        else if (print.op == PRINT_CHAR) {
            if (print.x1 instanceof InstNumber) {
                int c = ((InstNumber) print.x1).number;
                if (c == '\n') return "\\n";
                else return String.valueOf((char) c);
            }
            else return null;
        }
        else if (print.op == PRINT_STR) {
            return print.label.name;
        }
        else return null;
    }
}

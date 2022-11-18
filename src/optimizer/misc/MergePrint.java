package optimizer.misc;

import intercode.Label;
import intercode.Operand.InstNumber;
import intercode.Quaternion;

import java.util.List;

import static intercode.Quaternion.OperatorType.*;

public class MergePrint {
    public static void run(List<Quaternion> inter) {
        StringBuilder buff = new StringBuilder();
        for (int i = 0; i < inter.size(); i++) {
            Quaternion q = inter.get(i);
            String nowStr = getPrintString(q);
            String nextStr = i + 1 < inter.size() ? getPrintString(inter.get(i + 1)) : null;
            if (nowStr != null && nextStr != null) {
                buff.append(nowStr);
                while (nextStr != null) {
                    buff.append(nextStr);
                    inter.remove(i + 1);
                    nextStr = i + 1 < inter.size() ? getPrintString(inter.get(i + 1)) : null;
                }
                q.op = PRINT_STR;
                q.x1 = null;
                q.label = new Label(buff.toString());
                buff.delete(0, buff.length());
            }
        }
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

package optimizer.misc;

import intercode.Quaternion;

import java.util.ArrayList;
import java.util.List;

import static intercode.Quaternion.OperatorType.*;

// 由于文法限制，main 函数不能被调用
// 将 main 函数移动到 .text 正下方（移动到 enter_main 位置）
// main 中的所有 return，换为 exit
public class MoveMainFunc {
    public static void run(List<Quaternion> inter) {
        int mainStartIdx = -1, mainEndIdx = -1, enterMainIdx = -1;
        boolean inMainFunc = false;
        for (int i = 0; i < inter.size(); i++) {
            Quaternion q = inter.get(i);
            if (q.op == ENTER_MAIN) enterMainIdx = i;
            if (q.op == FUNC && q.label.name.equals("main")) {
                inMainFunc = true;
                mainStartIdx = i;
            }
            if (inMainFunc) {
                if (i == inter.size() - 1 || inter.get(i + 1).op == FUNC) {
                    mainEndIdx = i + 1;
                    break;
                }
            }
        }
        assert mainStartIdx >= 0 && mainEndIdx >= 0 && enterMainIdx >= 0;

        List<Quaternion> mainFunction = new ArrayList<>(inter.subList(mainStartIdx, mainEndIdx));
        for (int i = 0; i < mainFunction.size(); i++) {
            Quaternion q = mainFunction.get(i);
            if (q.op == SET_RETURN) mainFunction.remove(i--);
            if (q.op == RETURN) q.op = EXIT;
        }

        inter.subList(mainStartIdx, mainEndIdx).clear();
        inter.remove(enterMainIdx);
        inter.addAll(enterMainIdx, mainFunction);
    }
}

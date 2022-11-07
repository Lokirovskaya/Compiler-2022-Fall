package intercode;

import intercode.Operand.VirtualReg;
import mips.MipsCoder;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

import static intercode.Quaternion.OperatorType.*;

class ResultOutput {
    public static void output(InterCode inter, String filename) throws IOException {
        StringBuilder sb = new StringBuilder();
        inter.forEachItem(quater -> {
                    if (quater.op == FUNC || quater.op == LABEL)
                        sb.append(quater).append('\n');
                    else {
                        sb.append("  ").append(String.format("%-30s", quater));
                        List<Operand> allOperandList = new ArrayList<>();
                        allOperandList.add(quater.target);
                        allOperandList.add(quater.x1);
                        allOperandList.add(quater.x2);
                        if (quater.list != null)
                            allOperandList.addAll(quater.list);
                        for (Operand _o : allOperandList) {
                            if (_o instanceof VirtualReg) {
                                VirtualReg o = (VirtualReg) _o;
                                sb.append("   @").append(o.regID).append(':');
                                if (o.getRealReg(quater.id) >= 0) sb.append(MipsCoder.getRegName(o.getRealReg(quater.id)));
                                else sb.append('[').append(o.stackOffset).append(']');
                            }
                        }
                        sb.append('\n');
                    }
                }
        );
        Files.write(Paths.get(filename), sb.toString().trim().getBytes(StandardCharsets.UTF_8));
    }
}

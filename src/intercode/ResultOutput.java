package intercode;

import intercode.Operand.VirtualReg;
import mips.MipsCoder;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.List;
import java.util.stream.Collectors;

import static intercode.Quaternion.OperatorType.FUNC;
import static intercode.Quaternion.OperatorType.LABEL;

public class ResultOutput {
    private static final boolean SHOW_ALLOC_INFO = false;

    public static void output(List<Quaternion> inter, String filename) throws IOException {
        StringBuilder sb = new StringBuilder();
        inter.forEach(q -> {

                    if (q.op == FUNC || q.op == LABEL)
                        sb.append(String.format("%-32s", q));
                    else
                        sb.append(String.format("  %-30s", q));
                    if (SHOW_ALLOC_INFO) {
                        for (VirtualReg vreg : q.getAllVregList()) {
                            sb.append("   @").append(vreg.regID).append(':');
                            if (vreg.realReg >= 0) sb.append(MipsCoder.getRegName(vreg.realReg));
                            else {
                                if (vreg.isGlobal) sb.append("[G").append(vreg.stackOffset).append(']');
                                else sb.append('[').append(vreg.stackOffset).append(']');
                            }
                        }
                        if (q.activeRegSet != null)
                            sb.append("   active:").append(q.activeRegSet.stream()
                                    .map(reg -> MipsCoder.getRegName(reg))
                                    .collect(Collectors.toList())
                            );
                    }
                    if (q.isUselessAssign) sb.append(" (useless)");
                    sb.append('\n');
                }
        );
        Files.write(Paths.get(filename), sb.toString().getBytes(StandardCharsets.UTF_8));
    }
}

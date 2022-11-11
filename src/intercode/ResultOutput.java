package intercode;

import intercode.Operand.VirtualReg;
import mips.MipsCoder;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.stream.Collectors;

import static intercode.Quaternion.OperatorType.FUNC;
import static intercode.Quaternion.OperatorType.LABEL;

class ResultOutput {
    public static void output(InterCode inter, String filename) throws IOException {
        StringBuilder sb = new StringBuilder();
        inter.forEachItem(quater -> {
                    if (quater.op == FUNC || quater.op == LABEL)
                        sb.append(String.format("%-32s", quater));
                    else
                        sb.append(String.format("  %-30s", quater));
                    for (VirtualReg vreg : quater.getAllVregList()) {
                        sb.append("   @").append(vreg.regID).append(':');
                        if (vreg.realReg >= 0) sb.append(MipsCoder.getRegName(vreg.realReg));
                        else {
                            if (vreg.isGlobal) sb.append("[G").append(vreg.stackOffset).append(']');
                            else sb.append('[').append(vreg.stackOffset).append(']');
                        }
                    }
                    if (quater.activeRegSet != null)
                        sb.append("   active:").append(quater.activeRegSet.stream()
                                .map(reg -> MipsCoder.getRegName(reg))
                                .collect(Collectors.toList())
                        );
                    if (quater.isUselessAssign) sb.append(" (useless)");
                    sb.append('\n');
                }
        );
        Files.write(Paths.get(filename), sb.toString().getBytes(StandardCharsets.UTF_8));
    }
}

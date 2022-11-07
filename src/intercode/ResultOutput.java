package intercode;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;

import static intercode.Quaternion.OperatorType.*;

class ResultOutput {
    public static void output(InterCode inter, String filename) throws IOException {
        StringBuilder sb = new StringBuilder();
        inter.forEachItem(quater -> {
                    if (quater.op == FUNC || quater.op == LABEL)
                        sb.append(quater).append('\n');
                    else
                        sb.append("  ").append(quater).append('\n');
                }
        );
        Files.write(Paths.get(filename), sb.toString().trim().getBytes(StandardCharsets.UTF_8));
    }
}

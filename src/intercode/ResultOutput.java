package intercode;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;

class ResultOutput {
    public static void output(InterCode inter, String filename) throws IOException {
        StringBuilder sb = new StringBuilder();
        inter.forEachItem(quater -> {
                    if (quater.op == Quaternion.OperatorType.FUNC)
                        sb.append('\n').append(quater).append('\n');
                    else
                        sb.append(quater).append('\n');
                }
        );
        Files.write(Paths.get(filename), sb.toString().trim().getBytes(StandardCharsets.UTF_8));
    }
}

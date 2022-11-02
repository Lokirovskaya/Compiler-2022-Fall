package intercode;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;

class ResultOutput {
    public static void output(InterCode inter, String filename) throws IOException {
        StringBuilder sb = new StringBuilder();
        inter.forEach(p -> {
                    if (p.get().op == Quaternion.OperatorType.FUNC)
                        sb.append('\n').append(p.get().toString()).append('\n');
                    else
                        sb.append(p.get().toString()).append('\n');
                }
        );
        Files.write(Paths.get(filename), sb.toString().trim().getBytes(StandardCharsets.UTF_8));
    }
}

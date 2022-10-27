package mips;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class Mips {
    String code;
    String op, x1, x2, x3; // 对于 .data 段的数据，这几个字段是无意义的

    private static final String splitRegex = "[a-zA-Z_][a-zA-Z0-9_]*|\\$zero|\\$..|-?\\d+";
    private static final Pattern pattern = Pattern.compile(splitRegex);

    public Mips(String code) {
        this.code = code;
        Matcher matcher = pattern.matcher(code);
        while (matcher.find()) {
            if (op == null) op = matcher.group();
            else if (x1 == null) x1 = matcher.group();
            else if (x2 == null) x2 = matcher.group();
            else if (x3 == null) x3 = matcher.group();
        }
    }
}

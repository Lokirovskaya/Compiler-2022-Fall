package mips;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class Mips {
    String code;
    String[] args = new String[4]; // 对于 .data 段的数据，这几个字段是无意义的

    private static final String splitRegex = "[a-zA-Z_][a-zA-Z0-9_]*|\\$zero|\\$..|-?\\d+";
    private static final Pattern pattern = Pattern.compile(splitRegex);

    public Mips(String code) {
        this.code = code;
        Matcher matcher = pattern.matcher(code);
        while (matcher.find()) {
            if (args[0] == null) args[0] = matcher.group();
            else if (args[1] == null) args[1] = matcher.group();
            else if (args[2] == null) args[2] = matcher.group();
            else if (args[3] == null) args[3] = matcher.group();
        }
    }
}

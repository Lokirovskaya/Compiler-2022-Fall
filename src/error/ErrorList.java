package error;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.*;

import static error.Error.ErrorType.*;

public class ErrorList {
    private static final List<Error> errorList = new ArrayList<>();

    public static void init() {
        errorList.clear();
    }

    public static void output(String filename) throws IOException {
        StringBuilder sb = new StringBuilder();
        errorList.sort(Comparator.comparingInt(e -> e.lineNumber));
        for (Error e : errorList) {
            sb.append(String.format("%d %c\n", e.lineNumber, mapErrorIndex.get(e.type)));
        }
        String str = sb.toString();
        Files.write(Paths.get(filename), str.getBytes(StandardCharsets.UTF_8));
    }

    public static void alert() {
        errorList.sort(Comparator.comparingInt(e -> e.lineNumber));
        for (Error e : errorList) {
            System.err.printf("%s %d\n", e.type.name(), e.lineNumber);
        }
    }

    public static void add(Error.ErrorType type, int lineNumber) {
        errorList.add(new Error(type, lineNumber));
    }

    public static int size() {
        return errorList.size();
    }

    private static final Map<Error.ErrorType, Character> mapErrorIndex = new HashMap<>();

    static {
        mapErrorIndex.put(ILLEGAL_CHAR, 'a');
        mapErrorIndex.put(IDENTIFIER_DUPLICATE, 'b');
        mapErrorIndex.put(IDENTIFIER_UNDEFINE, 'c');
        mapErrorIndex.put(PARAM_COUNT_UNMATCH, 'd');
        mapErrorIndex.put(PARAM_TYPE_UNMATCH, 'e');
        mapErrorIndex.put(RETURN_EXPRESSION_WHEN_VOID, 'f');
        mapErrorIndex.put(MISSING_RETURN, 'g');
        mapErrorIndex.put(CHANGE_CONST, 'h');
        mapErrorIndex.put(MISSING_SEMICOLON, 'i');
        mapErrorIndex.put(MISSING_RIGHT_PAREN, 'j');
        mapErrorIndex.put(MISSING_RIGHT_BRACKET, 'k');
        mapErrorIndex.put(FORMAT_PARAM_COUNT_UNMATCH, 'l');
        mapErrorIndex.put(ILLEGAL_BREAK_CONTINUE, 'm');
    }
}

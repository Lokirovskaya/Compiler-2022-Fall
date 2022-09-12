public class ASCIICharacter {
     static boolean isAlpha(char c) {
        return ('a' <= c && c <= 'z') || ('A' <= c && c <= 'Z');
    }

     static boolean isDigit(char c) {
        return '0' <= c && c <= '9';
    }

     static boolean isWhiteSpace(char c) {
        return c <= 32 || c == 127;
    }

     static boolean isSingleOperator(char c) {
        return "+-*%;,()[]{}".indexOf(c) != -1;
    }

     static boolean isMultiOperatorStart(char c) {
        // != <= >= == && || // /*
        return "!<>=&|/".indexOf(c) != -1;
    }

     static boolean isIdentifierStart(char c) {
        return isAlpha(c) || c == '_';
    }

     static boolean isIdentifierChar(char c) {
        return isIdentifierStart(c) || isDigit(c);
    }
}

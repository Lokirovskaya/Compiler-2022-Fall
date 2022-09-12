public class ASCIIString {
    static boolean isIdentifier(String s) {
        if (!ASCIICharacter.isIdentifierStart(s.charAt(0))) return false;
        for (char c : s.toCharArray()) {
            if (!ASCIICharacter.isIdentifierChar(c)) return false;
        }
        return true;
    }

    static boolean isUnsignedInteger(String s) {
        for (char c : s.toCharArray()) {
            if (!ASCIICharacter.isDigit(c)) return false;
        }
        return true;
    }
}

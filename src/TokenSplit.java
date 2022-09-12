import java.text.CharacterIterator;
import java.text.StringCharacterIterator;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Function;

public class TokenSplit {
    static List<Token> getTokens(String code) {
        List<Token> result = new ArrayList<>();
        StringBuilder sb = new StringBuilder();
        CharacterIterator it = new StringCharacterIterator(code);
        final char EOF = CharacterIterator.DONE;
        int[] lineNumber = new int[]{1};
        Function<String, Token> getOneToken = (tokenString) -> {
            Token token = new Token();
            token.name = tokenString;
            token.type = TokenClassify.getTokenType(tokenString);
            token.lineNumber = lineNumber[0];
            return token;
        };
        Runnable collectCurrentTokenString = () -> {
            if (sb.length() != 0) {
                String tokenString = sb.toString();
                sb.delete(0, sb.length());
                result.add(getOneToken.apply(tokenString));
            }
        };
        Consumer<String> addNewTokenString = (s) -> result.add(getOneToken.apply(s));

        while (it.current() != EOF) {
            // Whitespace
            while (it.current() != EOF && ASCIICharacter.isWhiteSpace(it.current())) {
                if (it.current() == '\n') lineNumber[0]++;
                it.next();
            }
            collectCurrentTokenString.run();

            // Identifier
            if (it.current() != EOF && ASCIICharacter.isIdentifierStart(it.current())) {
                while (it.current() != EOF && ASCIICharacter.isIdentifierChar(it.current())) {
                    sb.append(it.current());
                    it.next();
                }
            }
            collectCurrentTokenString.run();

            // Number
            while (it.current() != EOF && ASCIICharacter.isDigit(it.current())) {
                sb.append(it.current());
                it.next();
            }
            collectCurrentTokenString.run();

            // String
            if (it.current() != EOF && it.current() == '"') {
                sb.append('"');
                it.next();
                boolean closed = false;
                while (it.current() != EOF && it.current() != '\n') {
                    sb.append(it.current());
                    if (it.current() == '"') {
                        closed = true;
                        it.next();
                        break;
                    }
                    it.next();
                }
                if (!closed) System.err.println("Not closed string");
            }
            collectCurrentTokenString.run();

            // Single Operator
            if (it.current() != EOF && ASCIICharacter.isSingleOperator(it.current())) {
                addNewTokenString.accept(String.valueOf(it.current()));
                it.next();
            }

            // Other Operator
            if (it.current() != EOF && ASCIICharacter.isMultiOperatorStart(it.current())) {
                char a = it.current();
                it.next();
                char b = it.current();
                it.next();
                // Multi Operator
                if ((a == '<' && b == '=') || (a == '>' && b == '=') ||
                        (a == '=' && b == '=') || (a == '!' && b == '=') ||
                        (a == '&' && b == '&') || (a == '|' && b == '|')) {
                    addNewTokenString.accept(String.valueOf(new char[]{a, b}));
                }
                // One Line Comment
                else if (a == '/' && b == '/') {
                    while (it.current() != EOF && it.current() != '\n') it.next();
                }
                // Multi Lines Comment
                else if (a == '/' && b == '*') {
                    CharacterIterator itNext;
                    itNext = (CharacterIterator) it.clone();
                    itNext.next();
                    boolean closed = false;
                    while (itNext.current() != EOF) {
                        if (it.current() == '\n') lineNumber[0]++;
                        if (it.current() == '*' && itNext.current() == '/') {
                            closed = true;
                            it.next();
                            it.next();
                            break;
                        }
                        it.next();
                        itNext.next();
                    }
                    if (!closed) System.err.println("Not closed multi comment");
                }
                // Single Operator
                else {
                    addNewTokenString.accept(String.valueOf(a));
                    it.previous();
                }
            }
        }
        collectCurrentTokenString.run();
        return result;
    }
}

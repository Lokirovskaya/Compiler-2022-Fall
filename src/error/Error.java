package error;

public class Error {
    ErrorType type;
    int lineNumber;

    public Error(ErrorType type, int lineNumber) {
        this.type = type;
        this.lineNumber = lineNumber;
    }

    public enum ErrorType{
        ILLEGAL_CHAR, IDENTIFIER_DUPLICATE, IDENTIFIER_UNDEFINE, PARAM_COUNT_UNMATCH,
        PARAM_TYPE_UNMATCH, RETURN_EXPRESSION_WHEN_VOID, MISSING_RETURN, CHANGE_CONST,
        MISSING_SEMICOLON, MISSING_RIGHT_PAREN, MISSING_RIGHT_BRACKET, FORMAT_PARAM_COUNT_UNMATCH,
        ILLEGAL_BREAK_CONTINUE
    }
}

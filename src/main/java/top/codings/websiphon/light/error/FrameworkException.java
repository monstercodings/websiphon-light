package top.codings.websiphon.light.error;

public class FrameworkException extends RuntimeException {
    public FrameworkException() {
        super();
    }

    public FrameworkException(String message) {
        super(message);
    }

    public FrameworkException(String message, Throwable cause) {
        super(message, cause);
    }

    public FrameworkException(Throwable cause) {
        super(cause);
    }

    protected FrameworkException(String message, Throwable cause, boolean enableSuppression, boolean writableStackTrace) {
        super(message, cause, enableSuppression, writableStackTrace);
    }
}

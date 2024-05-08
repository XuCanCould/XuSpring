package cn.xu.spring.exception;

/**
 * created by Xu on 2024/5/8 14:18.
 */
public class NestedRuntimeException extends RuntimeException {
    public NestedRuntimeException() {
    }

    public NestedRuntimeException(String message) {
        super(message);
    }

    public NestedRuntimeException(Throwable cause) {
        super(cause);
    }

    public NestedRuntimeException(String message, Throwable cause) {
        super(message, cause);
    }

}

package cn.xu.spring.exception;

/**
 * created by Xu on 2024/5/8 14:17.
 */
public class BeansException extends NestedRuntimeException {
    public BeansException() {}

    public BeansException(String message) {
        super(message);
    }

    public BeansException(Throwable cause) {
        super(cause);
    }

    public BeansException(String message, Throwable cause) {
        super(message, cause);
    }
}

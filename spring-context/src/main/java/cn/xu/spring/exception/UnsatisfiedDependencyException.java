package cn.xu.spring.exception;

/**
 * created by Xu on 2024/5/18 13:51.
 */
public class UnsatisfiedDependencyException extends BeanCreationException {
    public UnsatisfiedDependencyException() {
    }

    public UnsatisfiedDependencyException(String message) {
        super(message);
    }

    public UnsatisfiedDependencyException(String message, Throwable cause) {
        super(message, cause);
    }

    public UnsatisfiedDependencyException(Throwable cause) {
        super(cause);
    }

}


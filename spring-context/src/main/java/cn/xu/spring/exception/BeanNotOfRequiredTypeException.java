package cn.xu.spring.exception;

/**
 * created by Xu on 2024/5/8 20:24.
 */
public class BeanNotOfRequiredTypeException extends BeansException {
    public BeanNotOfRequiredTypeException() {
    }

    public BeanNotOfRequiredTypeException(String message) {
        super(message);
    }
}

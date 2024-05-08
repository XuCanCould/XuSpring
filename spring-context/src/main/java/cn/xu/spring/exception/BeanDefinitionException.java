package cn.xu.spring.exception;

/**
 * created by Xu on 2024/5/8 15:08.
 */
public class BeanDefinitionException extends BeansException{

    public BeanDefinitionException() {
    }

    public BeanDefinitionException(String message) {
        super(message);
    }

    public BeanDefinitionException(String message, Throwable cause) {
        super(message, cause);
    }

    public BeanDefinitionException(Throwable cause) {
        super(cause);
    }
}

package cn.xu.spring.exception;

/**
 * created by Xu on 2024/5/8 20:37.
 */
public class NoUniqueBeanDefinitionException extends BeanDefinitionException {
    public NoUniqueBeanDefinitionException() {}

    public NoUniqueBeanDefinitionException(String message) {
        super(message);
    }
}

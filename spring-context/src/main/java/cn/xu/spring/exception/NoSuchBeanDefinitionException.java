package cn.xu.spring.exception;

/**
 * created by Xu on 2024/5/18 20:24.
 */
public class NoSuchBeanDefinitionException extends BeanDefinitionException{

    public NoSuchBeanDefinitionException() {}

    public NoSuchBeanDefinitionException(String message) {
        super(message);
    }
}

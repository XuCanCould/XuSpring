package cn.xu.spring.exception;

/**
 * created by Xu on 2024/5/23 14:20.
 */
public class AopConfigException extends NestedRuntimeException{
    public AopConfigException() {}

    public AopConfigException(String msg, Throwable cause) {
        super(msg, cause);
    }

    public AopConfigException(String msg) {
        super(msg);
    }

    public AopConfigException(Throwable cause) {
        super(cause);
    }
}

package cn.xu.spring.exception;

/**
 * created by Xu on 2024/5/24 14:28.
 */
public class DataAccessException extends NestedRuntimeException{
    public DataAccessException() {
    }

    public DataAccessException(String msg) {
        super(msg);
    }

    public DataAccessException(Throwable cause) {
        super(cause);
    }

    public DataAccessException(String msg, Throwable cause) {
        super(msg, cause);
    }
}

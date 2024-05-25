package cn.xu.spring.exception;

/**
 * created by Xu on 2024/5/24 16:00.
 */
public class TransactionException extends DataAccessException {
    public TransactionException() {
    }

    public TransactionException(String msg) {
        super(msg);
    }

    public TransactionException(Throwable cause) {
        super(cause);
    }

    public TransactionException(String msg, Throwable cause) {
        super(msg, cause);
    }
}

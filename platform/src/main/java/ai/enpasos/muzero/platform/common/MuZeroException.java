package ai.enpasos.muzero.platform.common;

public class MuZeroException extends RuntimeException {
    public MuZeroException() {
        super();
    }

    public MuZeroException(String message) {
        super(message);
    }

    public MuZeroException(String message, Throwable cause) {
        super(message, cause);
    }

    public MuZeroException(Throwable cause) {
        super(cause);
    }


}

package shared.ports.persistence.exceptions;

public class NotInRepositoryException extends Exception {

    public NotInRepositoryException(String message) {
        super(message);
    }

    public NotInRepositoryException(String message, Throwable cause) {
        super(message, cause);
    }

    public NotInRepositoryException(Throwable cause) {
        super(cause);
    }
}

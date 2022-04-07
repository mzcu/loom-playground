package loom.sc.support;

public class StructuredTaskException extends RuntimeException {
    public StructuredTaskException(final Exception cause) {
        super(cause);
    }
}

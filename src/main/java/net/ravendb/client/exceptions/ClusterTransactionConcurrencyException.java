package net.ravendb.client.exceptions;

import net.ravendb.client.primitives.UseSharpEnum;

/**
 * This exception is raised when a concurrency conflict is encountered
 */
public class ClusterTransactionConcurrencyException extends ConcurrencyException {

    private ConcurrencyViolation[] concurrencyViolations;

    public ClusterTransactionConcurrencyException() {
    }

    public ClusterTransactionConcurrencyException(String message) {
        super(message);
    }

    public ClusterTransactionConcurrencyException(String message, Throwable cause) {
        super(message, cause);
    }

    public ConcurrencyViolation[] getConcurrencyViolations() {
        return concurrencyViolations;
    }

    public void setConcurrencyViolations(ConcurrencyViolation[] concurrencyViolations) {
        this.concurrencyViolations = concurrencyViolations;
    }

    public static class ConcurrencyViolation {
        private ViolationOnType type;
        private String id;
        private long expected;
        private long actual;

        public ViolationOnType getType() {
            return type;
        }

        public void setType(ViolationOnType type) {
            this.type = type;
        }

        public String getId() {
            return id;
        }

        public void setId(String id) {
            this.id = id;
        }

        public long getExpected() {
            return expected;
        }

        public void setExpected(long expected) {
            this.expected = expected;
        }

        public long getActual() {
            return actual;
        }

        public void setActual(long actual) {
            this.actual = actual;
        }
    }

    @UseSharpEnum
    public enum ViolationOnType {
        DOCUMENT,
        COMPARE_EXCHANGE
    }
}

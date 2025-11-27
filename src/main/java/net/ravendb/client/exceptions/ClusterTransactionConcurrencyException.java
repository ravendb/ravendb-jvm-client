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
        /**
         * Concurrency violation occured on {@link ViolationOnType}.
         */
        private ViolationOnType type;
        /**
         * The ID of which the concurrency check failed.
         */
        private String id;
        /**
         * The expected index for the concurrency check.
         */
        private long expected;
        /**
         * The actual index of the concurrency check.
         */
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
        /**
         * Concurrency violation occured on a document.
         */
        DOCUMENT,
        /**
         * Concurrency violation occured on a compare exchange
         */
        COMPARE_EXCHANGE
    }
}

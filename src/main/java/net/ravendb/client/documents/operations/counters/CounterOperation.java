package net.ravendb.client.documents.operations.counters;

import com.fasterxml.jackson.core.JsonGenerator;
import net.ravendb.client.primitives.SharpEnum;

import java.io.IOException;

/**
 * Represents a single counter operation, which defines actions such as incrementing a counter value, deleting a counter or retrieving a counter value for a specific document and counter name.
 */
public class CounterOperation {

    /**
     * The type of the counter operation to be performed.
     * Specifies the action, such as incrementing, deleting, or retrieving a counter value.
     *
     * <p><strong>Remarks:</strong> Valid types are defined in the {@link CounterOperationType} enum, including:</p>
     * <ul>
     *     <li>{@link CounterOperationType#INCREMENT}: Increments the counter by a specified value.</li>
     *     <li>{@link CounterOperationType#DELETE}: Deletes the counter.</li>
     *     <li>{@link CounterOperationType#GET}: Retrieves the value of the counter.</li>
     *     <li>{@link CounterOperationType#GET_ALL}: Retrieves all counters for a document.</li>
     * </ul>
     */
    private CounterOperationType type;
    /**
     * The name of the counter to be operated on.
     *
     * <p><strong>Remarks:</strong></p>
     * <ul>
     *     <li>The {@code counterName} field is mandatory for operations of type {@link CounterOperationType#INCREMENT},
     *         {@link CounterOperationType#DELETE}, and {@link CounterOperationType#GET}.</li>
     *     <li>For {@link CounterOperationType#GET_ALL}, the {@code counterName} field is not used and can be {@code null}.</li>
     *     <li>If {@code counterName} is required but not set, an exception will be thrown during parsing or execution.</li>
     * </ul>
     */
    private String counterName;
    /**
     * The value by which the counter should be incremented or decremented.
     *
     * <p><strong>Remarks:</strong></p>
     * <ul>
     *     <li>Used only for operations of type {@link CounterOperationType#INCREMENT}.</li>
     *     <li>For {@link CounterOperationType#PUT}, this specifies the exact value to set for the counter but is used internally and not intended for external use.</li>
     *     <li>For other operation types, this field is ignored.</li>
     * </ul>
     */
    private long delta;

    protected String changeVector;

    public void serialize(JsonGenerator generator) throws IOException {
        generator.writeStartObject();
        generator.writeObjectField("Type", SharpEnum.value(type));
        generator.writeStringField("CounterName", counterName);
        generator.writeNumberField("Delta", delta);
        generator.writeEndObject();
    }

    public CounterOperationType getType() {
        return type;
    }

    public void setType(CounterOperationType type) {
        this.type = type;
    }

    public String getCounterName() {
        return counterName;
    }

    public void setCounterName(String counterName) {
        this.counterName = counterName;
    }

    public long getDelta() {
        return delta;
    }

    public void setDelta(long delta) {
        this.delta = delta;
    }

    public String getChangeVector() {
        return changeVector;
    }

    public void setChangeVector(String changeVector) {
        this.changeVector = changeVector;
    }

    public static CounterOperation create(String counterName, CounterOperationType type) {
        CounterOperation operation = new CounterOperation();
        operation.setCounterName(counterName);
        operation.setType(type);
        return operation;
    }

    public static CounterOperation create(String counterName, CounterOperationType type, long delta) {
        CounterOperation operation = new CounterOperation();
        operation.setCounterName(counterName);
        operation.setType(type);
        operation.setDelta(delta);
        return operation;
    }
}

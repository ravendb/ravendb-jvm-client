package net.ravendb.client.documents.operations.counters;

import com.fasterxml.jackson.core.JsonGenerator;
import net.ravendb.client.documents.conventions.DocumentConventions;
import net.ravendb.client.primitives.SharpEnum;

import java.io.IOException;

public class CounterOperation {

    private CounterOperationType type;
    private String counterName;
    private long delta;

    protected String changeVector;

    public void serialize(JsonGenerator generator, DocumentConventions conventions) throws IOException {
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

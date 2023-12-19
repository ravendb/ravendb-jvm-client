package net.ravendb.client.documents.operations.queueSink;

import java.util.List;

public class QueueSinkScript {
    private String name;
    private List<String> queues;
    private String script;
    private boolean disabled;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public List<String> getQueues() {
        return queues;
    }

    public void setQueues(List<String> queues) {
        this.queues = queues;
    }

    public String getScript() {
        return script;
    }

    public void setScript(String script) {
        this.script = script;
    }

    public boolean isDisabled() {
        return disabled;
    }

    public void setDisabled(boolean disabled) {
        this.disabled = disabled;
    }
}

package net.ravendb.client.documents.operations.connectionStrings;

import net.ravendb.client.serverwide.ConnectionStringType;
import java.util.List;

public abstract class ConnectionString {
    private String name;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    @SuppressWarnings("SameReturnValue")
    public abstract ConnectionStringType getType();

    public boolean validate(List<String> errors) {
        if (errors == null) {
            throw new IllegalArgumentException("errors cannot be null");
        }

        int count = errors.size();

        validateImpl(errors);

        return count == errors.size();
    }

    protected abstract void validateImpl(List<String> errors);
}

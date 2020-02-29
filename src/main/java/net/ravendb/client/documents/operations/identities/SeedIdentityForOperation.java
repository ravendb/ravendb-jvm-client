package net.ravendb.client.documents.operations.identities;

import net.ravendb.client.documents.commands.SeedIdentityForCommand;
import net.ravendb.client.documents.conventions.DocumentConventions;
import net.ravendb.client.documents.operations.IMaintenanceOperation;
import net.ravendb.client.http.RavenCommand;
import org.apache.commons.lang3.StringUtils;

public class SeedIdentityForOperation implements IMaintenanceOperation<Long> {

    private final String _identityName;
    private final long _identityValue;
    private final boolean _forceUpdate;

    public SeedIdentityForOperation(String name, Long value) {
        this(name, value, false);
    }

    public SeedIdentityForOperation(String name, Long value, boolean forceUpdate) {
        if (StringUtils.isBlank(name)) {
            throw new IllegalArgumentException("The field name cannot be null or whitespace.");
        }

        _identityName = name;
        _identityValue = value;
        _forceUpdate = forceUpdate;
    }

    @Override
    public RavenCommand<Long> getCommand(DocumentConventions conventions) {
        return new SeedIdentityForCommand(_identityName, _identityValue, _forceUpdate);
    }
}

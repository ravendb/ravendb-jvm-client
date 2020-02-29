package net.ravendb.client.documents.operations.identities;

import net.ravendb.client.documents.commands.NextIdentityForCommand;
import net.ravendb.client.documents.conventions.DocumentConventions;
import net.ravendb.client.documents.operations.IMaintenanceOperation;
import net.ravendb.client.http.RavenCommand;
import org.apache.commons.lang3.StringUtils;

public class NextIdentityForOperation implements IMaintenanceOperation<Long> {
    private final String _identityName;

    public NextIdentityForOperation(String name) {
        if (StringUtils.isBlank(name)) {
            throw new IllegalArgumentException("The field name cannot be null or whitespace.");
        }

        _identityName = name;
    }

    @Override
    public RavenCommand<Long> getCommand(DocumentConventions conventions) {
        return new NextIdentityForCommand(_identityName);
    }
}

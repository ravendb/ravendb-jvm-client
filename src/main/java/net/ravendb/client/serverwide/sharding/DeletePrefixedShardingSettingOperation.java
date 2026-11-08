package net.ravendb.client.serverwide.sharding;

import net.ravendb.client.documents.conventions.DocumentConventions;
import net.ravendb.client.documents.operations.IVoidMaintenanceOperation;
import net.ravendb.client.http.VoidRavenCommand;
import org.apache.commons.lang3.StringUtils;

public class DeletePrefixedShardingSettingOperation implements IVoidMaintenanceOperation {

    private final String _prefix;

    public DeletePrefixedShardingSettingOperation(String prefix) {
        if (StringUtils.isEmpty(prefix)) {
            throw new IllegalArgumentException("prefix cannot be null or empty");
        }
        _prefix = prefix;
    }

    @Override
    public VoidRavenCommand getCommand(DocumentConventions conventions) {
        return new DeletePrefixedShardingSettingCommand(conventions, new PrefixedShardingSetting(_prefix));
    }

    private static class DeletePrefixedShardingSettingCommand extends PrefixedShardingCommand {

        public DeletePrefixedShardingSettingCommand(DocumentConventions conventions, PrefixedShardingSetting setting) {
            super(conventions, setting);
        }

        @Override
        protected PrefixedCommandType getCommandType() {
            return PrefixedCommandType.DELETE;
        }
    }
}

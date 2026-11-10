package net.ravendb.client.serverwide.sharding;

import net.ravendb.client.documents.conventions.DocumentConventions;
import net.ravendb.client.documents.operations.IVoidMaintenanceOperation;
import net.ravendb.client.http.VoidRavenCommand;

public class AddPrefixedShardingSettingOperation implements IVoidMaintenanceOperation {

    private final PrefixedShardingSetting _setting;

    public AddPrefixedShardingSettingOperation(PrefixedShardingSetting setting) {
        if (setting == null) {
            throw new IllegalArgumentException("setting cannot be null");
        }
        _setting = setting;
    }

    @Override
    public VoidRavenCommand getCommand(DocumentConventions conventions) {
        return new AddPrefixedShardingSettingCommand(conventions, _setting);
    }

    private static class AddPrefixedShardingSettingCommand extends PrefixedShardingCommand {

        public AddPrefixedShardingSettingCommand(DocumentConventions conventions, PrefixedShardingSetting setting) {
            super(conventions, setting);
        }

        @Override
        protected PrefixedCommandType getCommandType() {
            return PrefixedCommandType.ADD;
        }
    }
}

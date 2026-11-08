package net.ravendb.client.serverwide.operations.logs;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.node.ObjectNode;
import net.ravendb.client.documents.conventions.DocumentConventions;
import net.ravendb.client.http.ServerNode;
import net.ravendb.client.http.VoidRavenCommand;
import net.ravendb.client.json.ContentProviderHttpEntity;
import net.ravendb.client.serverwide.operations.IVoidServerOperation;
import org.apache.hc.client5.http.classic.methods.HttpPost;
import org.apache.hc.client5.http.classic.methods.HttpUriRequestBase;
import org.apache.hc.core5.http.ContentType;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

/**
 * Allows to set logs configuration.
 */
public class SetLogsConfigurationOperation implements IVoidServerOperation {

    private final Object _parameters;

    public static final class LogsConfiguration {
        private LogLevel minLevel;
        private List<LogFilter> filters = new ArrayList<>();
        private LogFilterAction logFilterDefaultAction = LogFilterAction.NEUTRAL;

        public LogsConfiguration() {
        }

        public LogsConfiguration(LogLevel minLevel) {
            this.minLevel = minLevel;
        }

        public LogLevel getMinLevel() {
            return minLevel;
        }

        public void setMinLevel(LogLevel minLevel) {
            this.minLevel = minLevel;
        }

        public List<LogFilter> getFilters() {
            return filters;
        }

        public void setFilters(List<LogFilter> filters) {
            this.filters = filters;
        }

        public LogFilterAction getLogFilterDefaultAction() {
            return logFilterDefaultAction;
        }

        public void setLogFilterDefaultAction(LogFilterAction logFilterDefaultAction) {
            this.logFilterDefaultAction = logFilterDefaultAction;
        }
    }

    public static final class MicrosoftLogsConfiguration {
        private LogLevel minLevel;

        public MicrosoftLogsConfiguration() {
        }

        public MicrosoftLogsConfiguration(LogLevel minLevel) {
            this.minLevel = minLevel;
        }

        public LogLevel getMinLevel() {
            return minLevel;
        }

        public void setMinLevel(LogLevel minLevel) {
            this.minLevel = minLevel;
        }
    }

    public static final class AdminLogsConfiguration {
        private LogLevel minLevel;
        private List<LogFilter> filters = new ArrayList<>();
        private LogFilterAction logFilterDefaultAction = LogFilterAction.NEUTRAL;

        public AdminLogsConfiguration() {
        }

        public AdminLogsConfiguration(LogLevel minLevel) {
            this.minLevel = minLevel;
        }

        public LogLevel getMinLevel() {
            return minLevel;
        }

        public void setMinLevel(LogLevel minLevel) {
            this.minLevel = minLevel;
        }

        public List<LogFilter> getFilters() {
            return filters;
        }

        public void setFilters(List<LogFilter> filters) {
            this.filters = filters;
        }

        public LogFilterAction getLogFilterDefaultAction() {
            return logFilterDefaultAction;
        }

        public void setLogFilterDefaultAction(LogFilterAction logFilterDefaultAction) {
            this.logFilterDefaultAction = logFilterDefaultAction;
        }
    }

    private static final class LogsParameters {
        private LogsConfiguration logs;
        private MicrosoftLogsConfiguration microsoftLogs;
        private AdminLogsConfiguration adminLogs;
        private boolean persist;

        public LogsConfiguration getLogs() {
            return logs;
        }

        public MicrosoftLogsConfiguration getMicrosoftLogs() {
            return microsoftLogs;
        }

        public AdminLogsConfiguration getAdminLogs() {
            return adminLogs;
        }

        public boolean isPersist() {
            return persist;
        }
    }

    public SetLogsConfigurationOperation(LogsConfiguration configuration) {
        this(configuration, false);
    }

    public SetLogsConfigurationOperation(LogsConfiguration configuration, boolean persist) {
        if (configuration == null) {
            throw new IllegalArgumentException("configuration cannot be null");
        }

        LogsParameters parameters = new LogsParameters();
        parameters.logs = configuration;
        parameters.persist = persist;
        _parameters = parameters;
    }

    public SetLogsConfigurationOperation(MicrosoftLogsConfiguration configuration) {
        this(configuration, false);
    }

    public SetLogsConfigurationOperation(MicrosoftLogsConfiguration configuration, boolean persist) {
        if (configuration == null) {
            throw new IllegalArgumentException("configuration cannot be null");
        }

        LogsParameters parameters = new LogsParameters();
        parameters.microsoftLogs = configuration;
        parameters.persist = persist;
        _parameters = parameters;
    }

    public SetLogsConfigurationOperation(AdminLogsConfiguration configuration) {
        this(configuration, false);
    }

    public SetLogsConfigurationOperation(AdminLogsConfiguration configuration, boolean persist) {
        if (configuration == null) {
            throw new IllegalArgumentException("configuration cannot be null");
        }

        LogsParameters parameters = new LogsParameters();
        parameters.adminLogs = configuration;
        parameters.persist = persist;
        _parameters = parameters;
    }

    /**
     * @param parameters Parameters
     * @deprecated Not supported by RavenDB 7.x servers; use {@link #SetLogsConfigurationOperation(LogsConfiguration, boolean)},
     * {@link #SetLogsConfigurationOperation(MicrosoftLogsConfiguration, boolean)} or {@link #SetLogsConfigurationOperation(AdminLogsConfiguration, boolean)}
     */
    @Deprecated
    public SetLogsConfigurationOperation(Parameters parameters) {
        if (parameters == null) {
            throw new IllegalArgumentException("Parameters cannot be null");
        }

        _parameters = parameters;
    }

    @Override
    public VoidRavenCommand getCommand(DocumentConventions conventions) {
        return new SetLogsConfigurationCommand(conventions, _parameters);
    }

    private static class SetLogsConfigurationCommand extends VoidRavenCommand {
        private final DocumentConventions _conventions;
        private final Object _parameters;

        public SetLogsConfigurationCommand(DocumentConventions conventions, Object parameters) {
            if (conventions == null) {
                throw new IllegalArgumentException("conventions cannot be null");
            }
            if (parameters == null) {
                throw new IllegalArgumentException("parameters cannot be null");
            }

            _parameters = parameters;
            _conventions = conventions;
        }

        @Override
        public HttpUriRequestBase createRequest(ServerNode node) {
            String url = node.getUrl() + "/admin/logs/configuration";

            HttpPost request = new HttpPost(url);
            request.setEntity(new ContentProviderHttpEntity(outputStream -> {
                try (JsonGenerator generator = createSafeJsonGenerator(outputStream)) {
                    ObjectNode config = mapper.valueToTree(_parameters);
                    generator.writeTree(config);
                }
            }, ContentType.APPLICATION_JSON, _conventions));
            return request;
        }
    }

    /**
     * @deprecated Not supported by RavenDB 7.x servers; use {@link LogsConfiguration}, {@link MicrosoftLogsConfiguration}
     * or {@link AdminLogsConfiguration} with the matching {@link SetLogsConfigurationOperation} constructor
     */
    @Deprecated
    public static class Parameters {
        private LogMode mode;
        private Duration retentionTime;
        private Long retentionSize;
        private boolean compress;
        private boolean persist;

        public Parameters(GetLogsConfigurationResult getLogs) {
            mode = getLogs.getMode();
            retentionTime = getLogs.getRetentionTime();
            retentionSize = getLogs.getRetentionSize();
            compress = getLogs.isCompress();
        }

        public Parameters() {
        }

        public Duration getRetentionTime() {
            return retentionTime;
        }

        public void setRetentionTime(Duration retentionTime) {
            this.retentionTime = retentionTime;
        }

        public Long getRetentionSize() {
            return retentionSize;
        }

        public void setRetentionSize(Long retentionSize) {
            this.retentionSize = retentionSize;
        }

        public boolean isCompress() {
            return compress;
        }

        public void setCompress(boolean compress) {
            this.compress = compress;
        }

        public LogMode getMode() {
            return mode;
        }

        public void setMode(LogMode mode) {
            this.mode = mode;
        }

        public boolean isPersist() {
            return persist;
        }

        public void setPersist(boolean persist) {
            this.persist = persist;
        }
    }
}

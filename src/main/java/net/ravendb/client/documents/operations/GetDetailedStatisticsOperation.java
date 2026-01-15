package net.ravendb.client.documents.operations;

import net.ravendb.client.documents.conventions.DocumentConventions;
import net.ravendb.client.http.RavenCommand;
import net.ravendb.client.http.ServerNode;
import org.apache.hc.client5.http.classic.methods.HttpUriRequestBase;
import org.apache.hc.client5.http.classic.methods.HttpGet;

import java.io.IOException;

/**
 * Retrieves detailed database statistics, providing in-depth information such as the count of compare exchange entries,
 * compare exchange tombstones, and time series deleted ranges.
 * It also includes base statistics like index information, storage sizes, and other relevant metrics.
 */
public class GetDetailedStatisticsOperation implements IMaintenanceOperation<DetailedDatabaseStatistics> {

    private final String _debugTag;

    /**
     * Inherits documentation from {@link GetDetailedStatisticsOperation}.
     */
    public GetDetailedStatisticsOperation() {
        this(null);
    }
    /**
     * Inherits documentation from {@link GetDetailedStatisticsOperation}.
     *
     * @param debugTag An optional tag for enhanced logging or debugging purposes.
     */
    public GetDetailedStatisticsOperation(String debugTag) {
        _debugTag = debugTag;
    }

    @Override
    public RavenCommand<DetailedDatabaseStatistics> getCommand(DocumentConventions conventions) {
         return new GetDetailedStatisticsCommand(_debugTag);
    }

    private static class GetDetailedStatisticsCommand extends RavenCommand<DetailedDatabaseStatistics> {
        private final String _debugTag;

        public GetDetailedStatisticsCommand(String debugTag) {
            super(DetailedDatabaseStatistics.class);
            _debugTag = debugTag;
        }

        @Override
        public HttpUriRequestBase createRequest(ServerNode node) {
            String url = node.getUrl() + "/databases/" + node.getDatabase() + "/stats/detailed";

            if (_debugTag != null) {
                url += "?" + _debugTag;
            }

            return new HttpGet(url);
        }

        @Override
        public void setResponse(String response, boolean fromCache) throws IOException {
            result = mapper.readValue(response, DetailedDatabaseStatistics.class);
        }

        @Override
        public boolean isReadRequest() {
            return true;
        }
    }
}

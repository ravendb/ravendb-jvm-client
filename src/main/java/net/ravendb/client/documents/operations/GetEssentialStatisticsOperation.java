package net.ravendb.client.documents.operations;

import net.ravendb.client.documents.conventions.DocumentConventions;
import net.ravendb.client.http.RavenCommand;
import net.ravendb.client.http.ServerNode;
import org.apache.hc.client5.http.classic.methods.HttpUriRequestBase;
import org.apache.hc.client5.http.classic.methods.HttpGet;

import java.io.IOException;

/**
 * Retrieves essential database statistics, focusing on critical metrics such as
 * the number of documents, document extensions, and essential index information.
 */
public class GetEssentialStatisticsOperation implements IMaintenanceOperation<EssentialDatabaseStatistics> {

    private final String debugTag;
    /**
     * Inherits documentation from {@link GetEssentialStatisticsOperation}.
     */
    public GetEssentialStatisticsOperation() {
        this(null);
    }
    /**
     * Inherits documentation from {@link GetEssentialStatisticsOperation}.
     *
     * @param debugTag An optional tag for enhanced logging or debugging purposes.
     */
    public GetEssentialStatisticsOperation(String debugTag) {
        this.debugTag = debugTag;
    }
    @Override
    public RavenCommand<EssentialDatabaseStatistics> getCommand(DocumentConventions conventions) {
        return new GetEssentialStatisticsCommand();
    }

    private static class GetEssentialStatisticsCommand extends RavenCommand<EssentialDatabaseStatistics> {
        public GetEssentialStatisticsCommand() {
            super(EssentialDatabaseStatistics.class);
        }

        @Override
        public HttpUriRequestBase createRequest(ServerNode node) {
            String url = node.getUrl() + "/databases/" + node.getDatabase() + "/stats/essential";

            return new HttpGet(url);
        }

        @Override
        public void setResponse(String response, boolean fromCache) throws IOException {
            result = mapper.readValue(response, resultClass);
        }

        @Override
        public boolean isReadRequest() {
            return true;
        }
    }
}

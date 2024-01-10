package net.ravendb.client.documents.operations;

import net.ravendb.client.documents.conventions.DocumentConventions;
import net.ravendb.client.http.ServerNode;
import net.ravendb.client.http.VoidRavenCommand;
import net.ravendb.client.primitives.Reference;
import org.apache.hc.client5.http.classic.methods.HttpUriRequestBase;
import org.apache.hc.client5.http.classic.methods.HttpGet;

import java.time.Duration;

public class DatabaseHealthCheckOperation implements IMaintenanceOperation {

    @Override
    public VoidRavenCommand getCommand(DocumentConventions conventions) {
        return new DatabaseHealthCheckCommand();
    }

    private static class DatabaseHealthCheckCommand extends VoidRavenCommand {
        public DatabaseHealthCheckCommand() {
            timeout = Duration.ofSeconds(15);
        }

        @Override
        public HttpUriRequestBase createRequest(ServerNode node) {
            String url = node.getUrl() + "/databases/" + node.getDatabase() + "/healthcheck";

            return new HttpGet(url);
        }
    }
}

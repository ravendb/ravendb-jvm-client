package net.ravendb.client.documents.operations;

import net.ravendb.client.documents.conventions.DocumentConventions;
import net.ravendb.client.http.RavenCommand;
import net.ravendb.client.http.ServerNode;
import net.ravendb.client.http.VoidRavenCommand;
import net.ravendb.client.primitives.Reference;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpRequestBase;

import java.time.Duration;

public class DatabaseHealthCheckOperation implements IMaintenanceOperation {

    @Override
    public RavenCommand getCommand(DocumentConventions conventions) {
        return new DatabaseHealthCheckCommand();
    }

    private static class DatabaseHealthCheckCommand extends VoidRavenCommand {
        public DatabaseHealthCheckCommand() {
            timeout = Duration.ofSeconds(15);
        }

        @Override
        public HttpRequestBase createRequest(ServerNode node, Reference<String> url) {
            url.value = node.getUrl() + "/databases/" + node.getDatabase() + "/healthcheck";

            return new HttpGet();
        }
    }
}

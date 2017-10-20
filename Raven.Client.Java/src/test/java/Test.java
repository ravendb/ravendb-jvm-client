import net.ravendb.client.documents.commands.GetNextOperationIdCommand;
import net.ravendb.client.documents.conventions.DocumentConventions;
import net.ravendb.client.http.RavenCommand;
import net.ravendb.client.http.RequestExecutor;
import net.ravendb.client.serverwide.operations.GetDatabaseNamesOperation;

import java.time.Duration;

public class Test {

//TODO: make sure it throws properly when server not found
    public static void main(String[] args) throws Exception {
        System.out.println(Duration.ofHours(4));

        RequestExecutor executor = RequestExecutor.create(new String[]{"http://localhost:8080"}, "db6dfd", new DocumentConventions());

        GetNextOperationIdCommand command = new GetNextOperationIdCommand();

        executor.execute(command);

        Long operationId = command.getResult();

        RavenCommand<String[]> databaseNamesCommand = new GetDatabaseNamesOperation(0, 10).getCommand(new DocumentConventions());

        executor.execute(databaseNamesCommand);

        System.out.println(String.join(", ", databaseNamesCommand.getResult()));

        System.out.println(operationId);

        Thread.sleep(200);

        GetNextOperationIdCommand command2 = new GetNextOperationIdCommand();

        executor.execute(command2);

        System.out.println(command2.getResult());
    }
}

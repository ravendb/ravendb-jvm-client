package net.ravendb.client.test.server.replication;

import net.ravendb.client.ReplicationTestBase;
import net.ravendb.client.documents.DocumentStore;
import net.ravendb.client.documents.IDocumentStore;
import net.ravendb.client.documents.operations.GetOngoingTaskInfoOperation;
import net.ravendb.client.documents.operations.ongoingTasks.*;
import net.ravendb.client.documents.operations.replication.PullReplicationAsSink;
import net.ravendb.client.documents.operations.replication.PullReplicationDefinition;
import net.ravendb.client.documents.operations.replication.PullReplicationDefinitionAndCurrentConnections;
import net.ravendb.client.documents.operations.replication.PutPullReplicationAsHubOperation;
import net.ravendb.client.documents.session.IDocumentSession;
import net.ravendb.client.infrastructure.entities.User;
import net.ravendb.client.serverwide.operations.ModifyOngoingTaskResult;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

public class PullReplicationTests extends ReplicationTestBase {

    @Test
    public void canDefinePullReplication() throws Exception {
        try (IDocumentStore store = getDocumentStore()) {
            PutPullReplicationAsHubOperation operation = new PutPullReplicationAsHubOperation("test");
            store.maintenance().forDatabase(store.getDatabase()).send(operation);
        }
    }

    @Test
    public void pullReplicationShouldWork() throws Exception {
        try (DocumentStore sink = getDocumentStore()) {
            try (DocumentStore hub = getDocumentStore()) {
                String name = "pull-replication" + sink.getDatabase();
                PutPullReplicationAsHubOperation putOperation = new PutPullReplicationAsHubOperation(name);
                hub.maintenance().forDatabase(hub.getDatabase()).send(putOperation);

                try (IDocumentSession s2 = hub.openSession()) {
                    s2.store(new User(), "foo/bar");
                    s2.saveChanges();
                }

                setupPullReplication(name, sink, hub);

                waitForDocumentToReplicate(sink, User.class,"foo/bar", 3_000);
            }
        }
    }

    @Test
    public void collectPullReplicationOngoingTaskInfo() throws Exception {
        try (DocumentStore sink = getDocumentStore()) {
            try (DocumentStore hub = getDocumentStore()) {
                String name = "pull-replication" + sink.getDatabase();

                PutPullReplicationAsHubOperation putOperation = new PutPullReplicationAsHubOperation(name);

                ModifyOngoingTaskResult hubTask = hub.maintenance().forDatabase(hub.getDatabase())
                        .send(putOperation);

                try (IDocumentSession s2 = hub.openSession()) {
                    s2.store(new User(), "foo/bar");
                    s2.saveChanges();
                }

                List<ModifyOngoingTaskResult> pullTasks = setupPullReplication(name, sink, hub);

                assertThat(waitForDocumentToReplicate(sink, User.class, "foo/bar", 3_000))
                        .isNotNull();

                OngoingTaskPullReplicationAsSink sinkResult = (OngoingTaskPullReplicationAsSink) sink.maintenance().send(
                        new GetOngoingTaskInfoOperation(pullTasks.get(0).getTaskId(), OngoingTaskType.PULL_REPLICATION_AS_SINK));

                assertThat(sinkResult.getDestinationDatabase())
                        .isEqualTo(hub.getDatabase());
                assertThat(sinkResult.getDestinationUrl())
                        .isEqualTo(hub.getUrls()[0]);
                assertThat(sinkResult.getTaskConnectionStatus())
                        .isEqualTo(OngoingTaskConnectionStatus.ACTIVE);

                PullReplicationDefinitionAndCurrentConnections hubResult = hub.maintenance().send(
                        new GetPullReplicationHubTasksInfoOperation(hubTask.getTaskId()));
                OngoingTaskPullReplicationAsHub ongoing = hubResult.getOngoingTasks().get(0);

                assertThat(ongoing.getDestinationDatabase())
                        .isEqualTo(sink.getDatabase());
                assertThat(ongoing.getDestinationUrl())
                        .isEqualTo(sink.getUrls()[0]);
                assertThat(ongoing.getTaskConnectionStatus())
                        .isEqualTo(OngoingTaskConnectionStatus.ACTIVE);
            }
        }
    }

    @Test
    public void deletePullReplicationFromHub() throws Exception {
        try (DocumentStore sink = getDocumentStore()) {
            try (DocumentStore hub = getDocumentStore()) {
                String name = "pull-replication" + sink.getDatabase();

                PutPullReplicationAsHubOperation putOperation = new PutPullReplicationAsHubOperation(name);
                ModifyOngoingTaskResult hubResult = hub.maintenance().forDatabase(hub.getDatabase()).send(putOperation);

                try (IDocumentSession session = hub.openSession()) {
                    session.store(new User(), "foo/bar");
                    session.saveChanges();
                }

                setupPullReplication(name, sink, hub);

                assertThat(waitForDocumentToReplicate(sink, User.class, "foo/bar", 3_000))
                        .isNotNull();

                deleteOngoingTask(hub, hubResult.getTaskId(), OngoingTaskType.PULL_REPLICATION_AS_HUB);

                try (IDocumentSession session = hub.openSession()) {
                    session.store(new User(), "foo/bar2");
                    session.saveChanges();
                }

                assertThat(waitForDocumentToReplicate(sink, User.class, "foo/bar2", 3_000))
                        .isNull();
            }
        }
    }

    @Test
    public void deletePullReplicationFromSink() throws Exception {
        try (DocumentStore sink = getDocumentStore()) {
            try (DocumentStore hub = getDocumentStore()) {
                String name = "pull-replication" + sink.getDatabase();

                PutPullReplicationAsHubOperation putOperation = new PutPullReplicationAsHubOperation(name);

                hub.maintenance().forDatabase(hub.getDatabase()).send(putOperation);

                try (IDocumentSession session = hub.openSession()) {
                    session.store(new User(), "foo/bar");
                    session.saveChanges();
                }

                List<ModifyOngoingTaskResult> sinkResult = setupPullReplication(name, sink, hub);

                assertThat(waitForDocumentToReplicate(sink, User.class, "foo/bar", 3_000))
                        .isNotNull();

                deleteOngoingTask(sink, sinkResult.get(0).getTaskId(), OngoingTaskType.PULL_REPLICATION_AS_SINK);

                try (IDocumentSession session = hub.openSession()) {
                    session.store(new User(), "foo/bar2");
                    session.saveChanges();
                }

                assertThat(waitForDocumentToReplicate(sink, User.class, "foo/bar2", 3_000))
                        .isNull();
            }
        }
    }

    @Test
    public void updatePullReplicationOnSink() throws Exception {
        try (DocumentStore sink = getDocumentStore()) {
            try (DocumentStore hub = getDocumentStore()) {
                try (DocumentStore hub2 = getDocumentStore()) {
                    String definitionName1 = "pull-replication " + hub.getDatabase();
                    String definitionName2 = "pull-replication " + hub2.getDatabase();

                    int timeout = 3_000;

                    hub.maintenance().forDatabase(hub.getDatabase()).send(new PutPullReplicationAsHubOperation(definitionName1));
                    hub2.maintenance().forDatabase(hub2.getDatabase()).send(new PutPullReplicationAsHubOperation(definitionName2));

                    try (IDocumentSession main = hub.openSession()) {
                        main.store(new User(), "hub1/1");
                        main.saveChanges();
                    }

                    List<ModifyOngoingTaskResult> pullTasks = setupPullReplication(definitionName1, sink, hub);
                    waitForDocumentToReplicate(sink, User.class,"hub1/1", timeout);

                    PullReplicationAsSink pull = new PullReplicationAsSink(hub2.getDatabase(), "ConnectionString2-" + sink.getDatabase(), definitionName2);
                    pull.setTaskId(pullTasks.get(0).getTaskId());

                    addWatcherToReplicationTopology(sink, pull, hub2.getUrls());

                    try (IDocumentSession main = hub.openSession()) {
                        main.store(new User(), "hub1/2");
                        main.saveChanges();
                    }

                    assertThat(waitForDocumentToReplicate(sink, User.class, "hub1/2", timeout))
                            .isNull();

                    try (IDocumentSession main = hub2.openSession()) {
                        main.store(new User(), "hub2");
                        main.saveChanges();
                    }

                    assertThat(waitForDocumentToReplicate(sink, User.class, "hub2", timeout))
                            .isNotNull();

                }
            }
        }
    }

    @Test
    public void updatePullReplicationOnHub() throws Exception {
        try (DocumentStore sink = getDocumentStore()) {
            try (DocumentStore hub = getDocumentStore()) {
                String definitionName = "pull-replication " + sink.getDatabase();

                ModifyOngoingTaskResult saveResult = hub.maintenance().forDatabase(hub.getDatabase())
                        .send(new PutPullReplicationAsHubOperation(definitionName));

                try (IDocumentSession main = hub.openSession()) {
                    main.store(new User(), "users/1");
                    main.saveChanges();
                }

                setupPullReplication(definitionName, sink, hub);
                assertThat(waitForDocumentToReplicate(sink, User.class, "users/1", 3_000))
                        .isNotNull();

                PullReplicationDefinition pullDefinition = new PullReplicationDefinition(definitionName);
                pullDefinition.setDelayReplicationFor(Duration.ofDays(1));
                pullDefinition.setTaskId(saveResult.getTaskId());

                hub.maintenance().forDatabase(hub.getDatabase()).send(new PutPullReplicationAsHubOperation(pullDefinition));

                try (IDocumentSession main = hub.openSession()) {
                    main.store(new User(), "users/2");
                    main.saveChanges();
                }

                assertThat(waitForDocumentToReplicate(sink, User.class, "users/2", 3_000))
                        .isNull();

                PullReplicationDefinition replicationDefinition = new PullReplicationDefinition(definitionName);
                replicationDefinition.setTaskId(saveResult.getTaskId());
                PutPullReplicationAsHubOperation hubOperation = new PutPullReplicationAsHubOperation(replicationDefinition);
                hub.maintenance().forDatabase(hub.getDatabase()).send(hubOperation);

                assertThat(waitForDocumentToReplicate(sink, User.class, "users/2", 20_000))
                        .isNotNull();
            }
        }
    }

    @Test
    public void disablePullReplicationOnSink() throws Exception {
        try (DocumentStore sink = getDocumentStore()) {
            try (DocumentStore hub = getDocumentStore()) {
                String definitionName = "pull-replication " + hub.getDatabase();
                int timeout = 3_000;

                hub.maintenance().forDatabase(hub.getDatabase())
                        .send(new PutPullReplicationAsHubOperation(definitionName));

                try (IDocumentSession main = hub.openSession()) {
                    main.store(new User(), "hub/1");
                    main.saveChanges();
                }

                List<ModifyOngoingTaskResult> pullTasks = setupPullReplication(definitionName, sink, hub);
                assertThat(waitForDocumentToReplicate(sink, User.class, "hub/1", timeout))
                        .isNotNull();

                PullReplicationAsSink pull = new PullReplicationAsSink(hub.getDatabase(), "ConnectionString-" + sink.getDatabase(), definitionName);
                pull.setDisabled(true);
                pull.setTaskId(pullTasks.get(0).getTaskId());

                addWatcherToReplicationTopology(sink, pull, hub.getUrls());

                try (IDocumentSession main = hub.openSession()) {
                    main.store(new User(), "hub/2");
                    main.saveChanges();
                }

                assertThat(waitForDocumentToReplicate(sink, User.class, "hub/2", timeout))
                        .isNull();

                pull.setDisabled(false);
                addWatcherToReplicationTopology(sink, pull, hub.getUrls());

                try (IDocumentSession main = hub.openSession()) {
                    main.store(new User(), "hub/3");
                    main.saveChanges();
                }

                assertThat(waitForDocumentToReplicate(sink, User.class, "hub/2", timeout))
                        .isNotNull();
                assertThat(waitForDocumentToReplicate(sink, User.class, "hub/3", timeout))
                        .isNotNull();
            }
        }
    }

    @Test
    public void disablePullReplicationOnHub() throws Exception {
        try (DocumentStore sink = getDocumentStore()) {
            try (DocumentStore hub = getDocumentStore()) {
                String definitionName = "pull-replication " + hub.getDatabase();

                PullReplicationDefinition pullDefinition = new PullReplicationDefinition(definitionName);
                ModifyOngoingTaskResult saveResult = hub.maintenance().forDatabase(hub.getDatabase())
                        .send(new PutPullReplicationAsHubOperation(pullDefinition));

                try (IDocumentSession main = hub.openSession()) {
                    main.store(new User(), "users/1");
                    main.saveChanges();
                }

                setupPullReplication(definitionName, sink, hub);
                assertThat(waitForDocumentToReplicate(sink, User.class, "users/1", 3_000))
                        .isNotNull();

                pullDefinition.setDisabled(true);
                pullDefinition.setTaskId(saveResult.getTaskId());

                hub.maintenance().forDatabase(hub.getDatabase())
                        .send(new PutPullReplicationAsHubOperation(pullDefinition));

                try (IDocumentSession main = hub.openSession()) {
                    main.store(new User(), "users/2");
                    main.saveChanges();
                }

                assertThat(waitForDocumentToReplicate(sink, User.class, "users/2", 3_000))
                        .isNull();

                pullDefinition.setDisabled(false);
                pullDefinition.setTaskId(saveResult.getTaskId());

                hub.maintenance().forDatabase(hub.getDatabase())
                        .send(new PutPullReplicationAsHubOperation(pullDefinition));

                assertThat(waitForDocumentToReplicate(sink, User.class, "users/2", 30_000))
                        .isNotNull();
            }
        }
    }

    @Test
    public void multiplePullExternalReplicationShouldWork() throws Exception {
        try (DocumentStore sink1 = getDocumentStore()) {
            try (DocumentStore sink2 = getDocumentStore()) {
                try (DocumentStore hub = getDocumentStore()) {
                    String name = "pull-replication " + hub.getDatabase();

                    hub.maintenance().forDatabase(hub.getDatabase())
                            .send(new PutPullReplicationAsHubOperation(name));

                    try (IDocumentSession session = hub.openSession()) {
                        session.store(new User(), "foo/bar");
                        session.saveChanges();
                    }

                    setupPullReplication(name, sink1, hub);
                    setupPullReplication(name, sink2, hub);

                    assertThat(waitForDocumentToReplicate(sink1, User.class, "foo/bar", 3_000))
                            .isNotNull();
                    assertThat(waitForDocumentToReplicate(sink2, User.class, "foo/bar", 3_000))
                            .isNotNull();
                }
            }
        }
    }

    public List<ModifyOngoingTaskResult> setupPullReplication(String remoteName, DocumentStore sink, DocumentStore... hub) {
        List<ModifyOngoingTaskResult> resList = new ArrayList<>();

        for (DocumentStore store : hub) {
            PullReplicationAsSink pull = new PullReplicationAsSink(store.getDatabase(), "ConnectionString-" + store.getDatabase(), remoteName);
            modifyReplicationDestination(pull);
            resList.add(addWatcherToReplicationTopology(sink, pull, store.getUrls()));
        }

        return resList;
    }
}

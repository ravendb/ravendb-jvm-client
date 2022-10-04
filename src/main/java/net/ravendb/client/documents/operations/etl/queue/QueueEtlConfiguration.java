package net.ravendb.client.documents.operations.etl.queue;

import net.ravendb.client.documents.operations.etl.EtlConfiguration;
import net.ravendb.client.documents.operations.etl.EtlType;

import java.util.List;

public class QueueEtlConfiguration extends EtlConfiguration<QueueConnectionString> {

    @Override
    public EtlType getEtlType() {
        return EtlType.QUEUE;
    }

    private List<EtlQueue> queues;

    private QueueBrokerType brokerType;

    private boolean skipAutomaticQueueDeclaration;

    public List<EtlQueue> getQueues() {
        return queues;
    }

    public void setQueues(List<EtlQueue> queues) {
        this.queues = queues;
    }

    public QueueBrokerType getBrokerType() {
        return brokerType;
    }

    public void setBrokerType(QueueBrokerType brokerType) {
        this.brokerType = brokerType;
    }

    public boolean isSkipAutomaticQueueDeclaration() {
        return skipAutomaticQueueDeclaration;
    }

    public void setSkipAutomaticQueueDeclaration(boolean skipAutomaticQueueDeclaration) {
        this.skipAutomaticQueueDeclaration = skipAutomaticQueueDeclaration;
    }

}

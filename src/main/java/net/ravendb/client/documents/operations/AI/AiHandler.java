package net.ravendb.client.documents.operations.AI;

@FunctionalInterface
public interface AiHandler<TArgs> {
    Object invoke(TArgs args);
}
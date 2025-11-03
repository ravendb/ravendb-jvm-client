package net.ravendb.client.documents.AI;

@FunctionalInterface
public interface AiHandler<TArgs> {
    Object invoke(TArgs args);
}
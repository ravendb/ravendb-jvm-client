package net.ravendb.client.primitives;

public interface EventHandler<T extends EventArgs> {
    /**
     * Handle event
     *
     * @param sender
     * @param event
     */
    void handle(Object sender, T event);
}

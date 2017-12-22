package net.ravendb.client.primitives;

public interface EventHandler<T extends EventArgs> {
    /**
     * Handle event
     * @param sender Event sender
     * @param event Event to send
     */
    void handle(Object sender, T event);
}

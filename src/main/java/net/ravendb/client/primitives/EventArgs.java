package net.ravendb.client.primitives;

/**
 * Parent class for all EventArgs
 */
public class EventArgs {
    //empty by design


    @SuppressWarnings("StaticInitializerReferencesSubClass")
    public final static VoidArgs EMPTY = new VoidArgs();
}

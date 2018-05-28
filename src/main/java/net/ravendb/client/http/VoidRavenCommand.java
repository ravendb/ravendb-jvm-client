package net.ravendb.client.http;

public abstract class VoidRavenCommand extends RavenCommand<Void> {

    protected VoidRavenCommand()
    {
        super(Void.class);
        responseType = RavenCommandResponseType.EMPTY;
    }

    @SuppressWarnings("SameReturnValue")
    @Override
    public boolean isReadRequest() {
        return false;
    }
}

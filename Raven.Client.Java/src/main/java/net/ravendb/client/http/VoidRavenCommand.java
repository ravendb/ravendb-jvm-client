package net.ravendb.client.http;

public abstract class VoidRavenCommand extends RavenCommand<Void> {

    protected VoidRavenCommand()
    {
        super(Void.class);
        responseType = RavenCommandResponseType.EMPTY;
    }

    @Override
    public boolean isReadRequest() {
        return false;
    }
}

package net.ravendb.client.documents.identity;

public class NextId {

    private long id;
    private String serverTag;

    private NextId() {
    }

    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    public String getServerTag() {
        return serverTag;
    }

    public void setServerTag(String serverTag) {
        this.serverTag = serverTag;
    }

    public static NextId create(long id, String serverTag) {
        NextId nextId = new NextId();
        nextId.setId(id);
        nextId.setServerTag(serverTag);

        return nextId;
    }
}

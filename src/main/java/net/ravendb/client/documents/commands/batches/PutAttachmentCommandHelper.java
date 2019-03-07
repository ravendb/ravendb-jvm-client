package net.ravendb.client.documents.commands.batches;

public class PutAttachmentCommandHelper {
    public static void throwStreamWasAlreadyUsed() {
        throw new IllegalStateException("It is forbidden to re-use the same InputStream for more than one attachment. Use a unique InputStream per put attachment command.");
    }
}

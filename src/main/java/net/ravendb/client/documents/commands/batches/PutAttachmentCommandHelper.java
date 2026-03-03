package net.ravendb.client.documents.commands.batches;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;

public class PutAttachmentCommandHelper {
    public static void throwStreamWasAlreadyUsed() {
        throw new IllegalStateException("It is forbidden to re-use the same InputStream for more than one attachment. Use a unique InputStream per put attachment command.");
    }

    public static byte[] toByteArray(InputStream in) throws IOException {
        ByteArrayOutputStream buffer = new ByteArrayOutputStream();
        byte[] data = new byte[4096];
        int nRead;

        while ((nRead = in.read(data)) != -1) {
            buffer.write(data, 0, nRead);
        }

        return buffer.toByteArray();
    }
}

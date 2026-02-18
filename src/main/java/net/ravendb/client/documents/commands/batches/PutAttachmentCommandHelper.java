package net.ravendb.client.documents.commands.batches;

import net.ravendb.client.documents.attachments.RemoteAttachmentFlags;
import net.ravendb.client.documents.operations.attachments.RemoteAttachmentParameters;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;

public class PutAttachmentCommandHelper {
    public static void throwStreamWasAlreadyUsed() {
        throw new IllegalStateException("It is forbidden to re-use the same InputStream for more than one attachment. Use a unique InputStream per put attachment command.");
    }

    public static boolean tryValidateStream(InputStream stream, RemoteAttachmentParameters parameters) {
        if (isRemoteStorageAttachment(parameters)) {
            if (stream != null) {
                throw new IllegalStateException("Stream must be null for remote storage attachments.");
            }
            return false;
        }

        if (stream == null) {
            throw new IllegalArgumentException("Attachment stream must not be null.");
        }

        try {
            stream.available();
        } catch (IOException e) {
            throw new IllegalStateException("Attachment stream is not readable.", e);
        }

        if (!stream.markSupported()) {
            throw new IllegalStateException("Attachment stream must be seekable (mark/reset supported).");
        }

        try {
            stream.mark(Integer.MAX_VALUE);
            stream.reset();
        } catch (IOException e) {
            throw new IllegalStateException("Attachment stream must be positioned at the beginning (position 0).", e);
        }

        return true;
    }

    private static boolean isRemoteStorageAttachment(RemoteAttachmentParameters parameters){
        if (parameters == null) {
            return false;
        }

        return parameters.getFlags() == RemoteAttachmentFlags.REMOTE;
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

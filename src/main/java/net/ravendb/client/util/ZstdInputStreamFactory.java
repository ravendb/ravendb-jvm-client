package net.ravendb.client.util;

import com.github.luben.zstd.ZstdInputStream;
import org.apache.http.client.entity.InputStreamFactory;

import java.io.IOException;
import java.io.InputStream;

public class ZstdInputStreamFactory implements InputStreamFactory {
    private static final ZstdInputStreamFactory INSTANCE = new ZstdInputStreamFactory();

    /**
     * Gets the singleton instance.
     *
     * @return the singleton instance.
     */
    public static ZstdInputStreamFactory getInstance() {
        return INSTANCE;
    }

    @Override
    public InputStream create(final InputStream inputStream) throws IOException {
        ZstdInputStream zstdInputStream = new ZstdInputStream(inputStream);
        zstdInputStream.setContinuous(true);
        return zstdInputStream;
    }

}

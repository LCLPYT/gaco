package work.lclpnet.gaco.asset;

import java.io.Closeable;
import java.io.IOException;
import java.io.InputStream;

public record AssetStreamResource(InputStream resource, boolean cached) implements Closeable {

    @Override
    public void close() throws IOException {
        resource.close();
    }
}

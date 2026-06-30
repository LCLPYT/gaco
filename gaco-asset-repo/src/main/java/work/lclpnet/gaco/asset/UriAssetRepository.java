package work.lclpnet.gaco.asset;

import com.google.common.collect.Iterators;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import work.lclpnet.gaco.asset.util.FileUtil;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.Collections;

public class UriAssetRepository implements AssetRepository {

    private final URI root;
    private final Logger logger;

    public UriAssetRepository(URI root, Logger logger) {
        this.root = root;
        this.logger = logger;
    }

    @Override
    public Iterable<AssetUriResource> getUris(AssetPath path, AssetRequestOptions options) {
        try {
            URI uri = uri(path);

            if (!exists(uri)) {
                logger.debug("Local asset '{}' does not exist at '{}'", path, uri);
                return Collections::emptyIterator;
            }

            AssetUriResource res = new AssetUriResource(uri, false);

            return () -> Iterators.singletonIterator(res);
        } catch (IOException e) {
            logger.debug("Failed to get uri for asset '{}'", path, e);
            return Collections::emptyIterator;
        }
    }

    private boolean exists(URI uri) {
        if (uri.getHost() != null) {
            // remote uri, existence cannot be checked cheaply
            return true;
        }

        Path path;

        try {
            path = uri.getScheme() != null ? Path.of(uri) : Path.of(uri.getPath());
        } catch (RuntimeException e) {
            // not a local file system path, leave it to the consumer
            return true;
        }

        return Files.exists(path);
    }

    @Override
    public AssetStreamResource getStream(AssetPath path, AssetRequestOptions options) throws IOException {
        URI uri = uri(path);
        URL url = uri.toURL();
        InputStream in = url.openStream();

        return new AssetStreamResource(in, false);
    }

    public @NotNull URI uri(AssetPath path) throws IOException {
        String encodedPath = AssetPath.of(Arrays.stream(path.segments())
                .map(FileUtil::encodeURIComponent)
                .toArray(String[]::new))
                .toString();

        URI uri = root.resolve(encodedPath);

        if (!uri.getPath().startsWith(root.getPath())) {
            throw new IOException("Path outside of repository");
        }

        return uri;
    }

    @Override
    public String toString() {
        return "UriAssetRepository{root=%s}".formatted(root);
    }
}

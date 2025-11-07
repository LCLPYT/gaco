package work.lclpnet.gaco.asset;

import com.google.common.collect.Iterators;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import work.lclpnet.gaco.asset.cache.AssetCache;

import java.io.IOException;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collections;

public class CacheAssetRepository implements AssetRepository {

    private final AssetCache cache;
    private final AssetRepository upstream;
    private final int ttlSeconds;
    private final Logger logger;

    public CacheAssetRepository(AssetCache cache, AssetRepository upstream, int ttlSeconds, Logger logger) {
        this.cache = cache;
        this.upstream = upstream;
        this.ttlSeconds = ttlSeconds;
        this.logger = logger;
    }

    @Override
    public AssetStreamResource getStream(AssetPath path, AssetRequestOptions options) throws IOException {
        if (options.preferUncached()) {
            logger.debug("Cache is disable for resource '{}', fetching from upstream {} ...", path, upstream);
            return upstream.getStream(path, options);
        }

        var cachedInfo = cache.getCacheInfo(path).orElse(null);
        var cachedPath = cachedInfo != null ? cachedInfo.path() : null;

        if (cachedPath != null && cachedInfo.valid()) {
            logger.debug("Using cached asset from '{}'", cachedPath);
            return new AssetStreamResource(Files.newInputStream(cachedPath), true);
        }

        logger.debug("Cache miss for asset '{}', fetching from upstream {} ...", path, upstream);

        var finalPath = cachedPath;

        try (var in = upstream.getStream(path, options)) {
            try {
                finalPath = cache.cache(path, in.resource(), ttlSeconds);
                logger.debug("Asset '{}' has been cached to {}", path, cachedPath);
            } catch (IOException e) {
                logger.error("Failed to cache asset '{}', refetching uncached...", path, e);
            }
        } catch (IOException e) {
            logger.debug("Failed to retrieve streams from upstream for '{}', trying to use old cached version instead...", path, e);

            if (cachedPath != null) {
                logger.debug("Found older version of the asset in the cache");
                finalPath = cachedPath;
            }
        }

        if (finalPath == null) {
            // context lost, have to fetch again...
            return upstream.getStream(path, options);
        }

        return new AssetStreamResource(Files.newInputStream(finalPath), false);
    }

    /**
     * Fetches {@link URI}s to a cached asset.
     * If no valid cached asset exists, the upstream is queried for the first existing asset, which is then cached.
     * If caching didn't work for any existing upstream asset, the upstream result is returned.
     * @param path The {@link AssetPath} towards the asset.
     * @param options The {@link AssetRequestOptions} for the request.
     * @return An {@link Iterable} of matching asset {@link URI}s.
     */
    @Override
    public Iterable<AssetUriResource> getUris(AssetPath path, AssetRequestOptions options) {
        if (options.preferUncached()) {
            var fallbackCachedPath = cache.getCacheInfo(path)
                    .map(AssetCache.CacheInfo::path)
                    .orElse(null);

            return getFreshUris(path, options, fallbackCachedPath);
        }

        return getCachedUris(path, options);
    }

    private Iterable<AssetUriResource> getCachedUris(AssetPath path, AssetRequestOptions options) {
        var cachedInfo = cache.getCacheInfo(path).orElse(null);
        Path cachedPath = cachedInfo != null ? cachedInfo.path() : null;

        if (cachedPath != null && cachedInfo.valid()) {
            logger.debug("Using uri for cached asset from '{}'", cachedPath);
            return wrap(cachedPath, true);
        }

        logger.debug("Cache miss for asset uri '{}', fetching from upstream {} ...", path, upstream);

        return getFreshUris(path, options, cachedPath);
    }

    private @NotNull Iterable<AssetUriResource> getFreshUris(AssetPath path, AssetRequestOptions options, @Nullable Path fallbackPath) {
        Iterable<AssetUriResource> uris = upstream.getUris(path, options);
        Path freshCachedPath = cacheFirstValid(path, uris);

        if (freshCachedPath != null) {
            logger.debug("Asset '{}' has been cached to {} from uri request", path, freshCachedPath);

            return wrap(freshCachedPath, false);
        }

        if (fallbackPath != null) {
            logger.debug("Falling back to older cached uri of '{}'", path);

            return wrap(fallbackPath, true);
        }

        return uris;
    }

    private @Nullable Path cacheFirstValid(AssetPath path, Iterable<AssetUriResource> uris) {
        for (AssetUriResource res : uris) {
            try {
                return cache.cache(path, res.resource(), ttlSeconds).orElse(null);
            } catch (IOException e) {
                logger.debug("Failed to cache asset '{}', skipping...", path, e);
            }
        }

        return null;
    }

    private Iterable<AssetUriResource> wrap(Path path, boolean cached) {
        URI uri;

        try {
            uri = path.toUri();
        } catch (Throwable t) {
            logger.error("Failed to convert path '{}' to uri", path, t);
            return Collections::emptyIterator;
        }

        var res = new AssetUriResource(uri, cached);

        return () -> Iterators.singletonIterator(res);
    }
}

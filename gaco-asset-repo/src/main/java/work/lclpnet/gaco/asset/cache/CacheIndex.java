package work.lclpnet.gaco.asset.cache;

public interface CacheIndex extends AutoCloseable {

    boolean isEntryInvalid(String path);

    void updateEntry(String path, int ttlSeconds);

    void invalidate(String path);
}

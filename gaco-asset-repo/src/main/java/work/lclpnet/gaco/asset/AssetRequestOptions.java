package work.lclpnet.gaco.asset;

public record AssetRequestOptions(boolean disableCacheRead) {

    public static final AssetRequestOptions DEFAULT = new AssetRequestOptions(false);

    public AssetRequestOptions withDisableCacheRead(boolean disableCacheRead) {
        return new AssetRequestOptions(disableCacheRead);
    }
}

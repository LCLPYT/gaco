package work.lclpnet.gaco.asset;

public record AssetRequestOptions(boolean preferUncached) {

    public static final AssetRequestOptions DEFAULT = new AssetRequestOptions(false);

    public AssetRequestOptions withPreferUncached(boolean preferUncached) {
        return new AssetRequestOptions(preferUncached);
    }
}

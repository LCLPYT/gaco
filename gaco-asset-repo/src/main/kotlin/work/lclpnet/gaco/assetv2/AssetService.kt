package work.lclpnet.gaco.assetv2

import java.io.InputStream
import java.net.URI

class AssetService(
    val repos: List<AssetRepo>,
) {
    suspend fun get(asset: String, file: String): AssetFetchResult {
        // first resolve an asset version to use
        val resolved = resolveAsset(asset) ?: return AssetFetchResult.AssetNotFound

        // now find file from the resolved asset version
        val path = "${resolved.path}/@${resolved.version.id}/$file"

        val data = resolved.repo.getFile(path)

        return AssetFetchResult.Success(data)
    }

    suspend fun getUri(asset: String, file: String): AssetFetchUriResult {
        // first resolve an asset version to use
        val resolved = resolveAsset(asset) ?: return AssetFetchUriResult.AssetNotFound

        // now find file from the resolved asset version
        val path = "${resolved.path}/@${resolved.version.id}/$file"

        val uri = resolved.repo.getUri(path)

        return AssetFetchUriResult.Success(uri)
    }

    private suspend fun resolveAsset(path: String): ResolvedAsset? {
        return repos.mapNotNull { repo ->
            val asset = repo.getAsset(path)

            val version = pickAssetVersion(asset)
                ?: return@mapNotNull null  // no compatible asset version

            ResolvedAsset(
                path = path,
                version = version,
                repo = repo,
            )
        }.firstOrNull()
    }
    
    private fun pickAssetVersion(asset: Asset): AssetVersion? {
        if (asset.versions.isEmpty()) {
            return null
        }

        val versioningScheme = detectVersioningScheme(asset)
            ?: throw IllegalStateException("impossible state")

        val versionComparator = Comparator<AssetVersion> { x, y ->
            versioningScheme.compareRaw(y.id, x.id)  // descending
        }

        return asset.versions
            .filter { assetConstraintsMatch(it) }
            .sortedWith(versionComparator)
            .firstOrNull()
    }
    
    private fun assetConstraintsMatch(version: AssetVersion): Boolean {
        // TODO("eval depends")
        return true
    }
}

private fun detectVersioningScheme(asset: Asset): VersioningScheme<out Any>? {
    // ensure consistent versioning scheme
    return asset.versions.fold(null) { prev, version ->
        val scheme = VersioningScheme.detect(version.id)
            ?: throw IllegalStateException("Unsupported version format: ${version.id}")

        if (prev == null || prev == scheme) {
            scheme
        } else {
            throw IllegalStateException("Inconsistent version format: version ${version.id} implies format $scheme, but previous entries implied $prev")
        }
    }
}

sealed interface AssetFetchResult {
    data class Success(val data: InputStream) : AssetFetchResult
    object AssetNotFound : AssetFetchResult
}

sealed interface AssetFetchUriResult {
    data class Success(val uri: URI) : AssetFetchUriResult
    object AssetNotFound : AssetFetchUriResult
}

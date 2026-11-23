package work.lclpnet.gaco.assetv2

import com.charleskorn.kaml.Yaml
import com.charleskorn.kaml.decodeFromStream
import kotlinx.coroutines.*
import work.lclpnet.gaco.assetv2.model.Asset
import work.lclpnet.gaco.assetv2.model.AssetListing
import work.lclpnet.gaco.assetv2.model.AssetVersion
import work.lclpnet.gaco.assetv2.resolved.ResolvedAsset
import work.lclpnet.gaco.assetv2.resolved.ResolvedListing
import work.lclpnet.gaco.assetv2.resolved.ResolvedListingItem
import java.io.InputStream
import java.net.URI

class AssetService(
    val repos: List<AssetRepo>,
) {

    /**
     * Open an input stream to a file of a versioned asset.
     * @param asset The asset path, without the asset version and without the target file.
     * @param file The file to open within the asset version.
     * @return An input stream with the assets file content or an error result if no asset could be found.
     */
    suspend fun get(asset: String, file: String): AssetFetchResult {
        val resolved = resolveAsset(asset) ?: return AssetFetchResult.AssetNotFound

        val data = resolved.repo.getFile(resolved.filePath(file))

        return AssetFetchResult.Success(data)
    }

    /**
     * Get a URI pointing at a file content of a versioned asset.
     * Useful, if the asset content is not consumed immediately, as a URI holds no open connection.
     * @param asset The asset path, without the asset version and without the target file.
     * @param file The file to open within the asset version.
     * @return A URI pointing to the asset file content or an error result if no asset could be found.
     */
    suspend fun getUri(asset: String, file: String): AssetFetchUriResult {
        val resolved = resolveAsset(asset) ?: return AssetFetchUriResult.AssetNotFound

        val uri = resolved.repo.getUri(resolved.filePath(file))

        return AssetFetchUriResult.Success(uri)
    }

    suspend fun getListing(assetPath: String): ResolvedListing {
        data class RepoListing(
            val listing: AssetListing,
            val repo: AssetRepo,
        )

        val items = coroutineScope {
            repos.map { repo ->
                async {
                    val asset = repo.getAsset(assetPath)

                    val version = pickAssetVersion(asset)
                        ?: return@async null  // no compatible asset version

                    val path = ResolvedAsset.buildFilePath(
                        assetPath = assetPath,
                        version = version.id,
                        file = "index.yml",
                    )

                    val listing = withContext(Dispatchers.IO) {
                        val source = repo.getFile(path)
                        Yaml.default.decodeFromStream<AssetListing>(source)
                    }

                    RepoListing(listing, repo)
                }
            }.awaitAll()
                .filterNotNull()
                .flatMap { repoListing ->
                    repoListing.listing.items.map {
                        ResolvedListingItem(
                            item = it,
                            repo = repoListing.repo,
                        )
                    }
                }
                .distinctBy { it.item.asset }
                .toSet()
        }

        return ResolvedListing(items)
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

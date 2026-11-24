package work.lclpnet.gaco.assetv2

import com.charleskorn.kaml.Yaml
import com.charleskorn.kaml.decodeFromStream
import kotlinx.coroutines.*
import org.slf4j.Logger
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
    val logger: Logger,
) {

    /**
     * Open an input stream to a file of a versioned asset.
     * @param asset The asset path, without the asset version and without the target file.
     * @param file The file to open within the asset version.
     * @return An input stream with the assets file content or an error result if no asset could be found.
     */
    suspend fun get(asset: String, file: String): AssetFetchResult {
        logger.debug("AssetService::get {} {}", asset, file)

        val resolved = resolveAsset(asset) ?: return AssetFetchResult.AssetNotFound

        logger.debug("AssetService::get resolved asset {}", resolved)

        val path = resolved.filePath(file)
        val data = resolved.repo.getFile(path)

        logger.debug("AssetService::get successfully fetched {}", path)

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
        logger.debug("AssetService::getUri {} {}", asset, file)

        val resolved = resolveAsset(asset) ?: return AssetFetchUriResult.AssetNotFound

        logger.debug("AssetService::getUri resolved asset {}", resolved)

        val uri = resolved.repo.getUri(resolved.filePath(file))

        logger.debug("AssetService::getUri successfully resolved URI {}", uri)

        return AssetFetchUriResult.Success(uri)
    }

    suspend fun getListing(assetPath: String): ResolvedListing {
        logger.debug("AssetService::getListing {}", assetPath)

        data class RepoListing(
            val listing: AssetListing,
            val repo: AssetRepo,
        )

        val repoListings = coroutineScope {
            repos.map { repo ->
                async {
                    val asset = repo.getAsset(assetPath)

                    logger.debug("AssetService::getListing found asset {} in repo {}", asset, repo)

                    val version = pickAssetVersion(asset, assetPath) ?: run {
                        logger.debug("AssetService::getListing no compatible versions found for asset {} in repo {}", asset, repo)
                        return@async null
                    }

                    logger.debug("AssetService::getListing picked {} version {}", asset, version.id)

                    val path = ResolvedAsset.buildFilePath(
                        assetPath = assetPath,
                        version = version.id,
                        file = "index.yml",
                    )

                    val listing = withContext(Dispatchers.IO) {
                        val source = repo.getFile(path)
                        Yaml.default.decodeFromStream<AssetListing>(source)
                    }

                    logger.debug("AssetService::getListing fetched listing {} from repo {}: {}", assetPath, repo, listing)

                    RepoListing(listing, repo)
                }
            }.awaitAll().filterNotNull()
        }

        val resolved = repoListings.flatMap { repoListing ->
            repoListing.listing.items.map {
                ResolvedListingItem(
                    item = it,
                    repo = repoListing.repo,
                )
            }
        }

        logger.debug("AssetService::getListing resolved listings: {}", resolved)

        val deduped = resolved.distinctBy { it.item.asset }.toSet()

        logger.debug("AssetService::getListing deduped listings: {}", deduped)

        return ResolvedListing(deduped)
    }

    private suspend fun resolveAsset(path: String): ResolvedAsset? {
        logger.debug("AssetService::resolveAsset {}", path)

        return repos.mapNotNull { repo ->
            val asset = repo.getAsset(path)

            logger.debug("AssetService::resolveAsset found asset {} in repo {}", asset, repo)

            val version = pickAssetVersion(asset, path)
                ?: return@mapNotNull null  // no compatible asset version

            ResolvedAsset(
                path = path,
                version = version,
                repo = repo,
            )
        }.firstOrNull()
    }
    
    private fun pickAssetVersion(asset: Asset, assetPath: String): AssetVersion? {
        logger.debug("AssetService::pickAssetVersion {} {}", assetPath, asset)

        if (asset.versions.isEmpty()) {
            logger.debug("AssetService::pickAssetVersion no versions configured for asset {}", assetPath)
            return null
        }

        val versioningScheme = detectVersioningScheme(asset)
            ?: throw IllegalStateException("impossible state")

        logger.debug("AssetService::pickAssetVersion detected versioning scheme {}", versioningScheme)

        val versionComparator = Comparator<AssetVersion> { x, y ->
            versioningScheme.compareRaw(y.id, x.id)  // descending
        }

        return asset.versions
            .filter { version ->
                assetConstraintsMatch(version).also { match ->
                    logger.debug("AssetService::pickAssetVersion constraints ${if (match) "" else "don't "}match for asset {} version {}", assetPath, version)
                }
            }
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

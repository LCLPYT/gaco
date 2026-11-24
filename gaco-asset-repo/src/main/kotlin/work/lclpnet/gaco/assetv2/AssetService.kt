package work.lclpnet.gaco.assetv2

import com.charleskorn.kaml.Yaml
import com.charleskorn.kaml.decodeFromStream
import kotlinx.coroutines.*
import org.slf4j.Logger
import work.lclpnet.gaco.assetv2.model.Asset
import work.lclpnet.gaco.assetv2.model.AssetListing
import work.lclpnet.gaco.assetv2.model.AssetVersion
import work.lclpnet.gaco.assetv2.model.toListingItem
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

        val resolved = when (val res = resolveAsset(asset)) {
            is AssetResolveResult.Success -> res.resolved
            is AssetResolveResult.AssetNotFound -> return AssetFetchResult.AssetNotFound
            is AssetResolveResult.NoCompatibleVersionFound -> return AssetFetchResult.NoCompatibleVersionFound
        }

        logger.debug("AssetService::get resolved asset {}", resolved)

        val path = resolved.filePath(file)

        val data = resolved.repo.getFile(path) ?: run {
            logger.debug("AssetService::get file {} not found for asset {}", file, resolved)
            return AssetFetchResult.FileNotFound
        }

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

        val resolved = when (val res = resolveAsset(asset)) {
            is AssetResolveResult.Success -> res.resolved
            is AssetResolveResult.AssetNotFound -> return AssetFetchUriResult.AssetNotFound
            is AssetResolveResult.NoCompatibleVersionFound -> return AssetFetchUriResult.NoCompatibleVersionFound
        }

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
                    val asset = repo.getAsset(assetPath) ?: run {
                        logger.debug("AssetService::getListing didn't find asset {} in repo {}", assetPath, repo)
                        return@async null
                    }

                    logger.debug("AssetService::getListing found asset {} in repo {}", assetPath, repo)

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
                        repo.getFile(path)?.let {
                            Yaml.default.decodeFromStream<AssetListing>(it)
                        }
                    } ?: run {
                        logger.debug("AssetService::getListing listing file {} not found in repo {}", path, repo)
                        return@async null
                    }

                    logger.debug("AssetService::getListing fetched listing {} from repo {}: {}", assetPath, repo, listing)

                    RepoListing(listing, repo)
                }
            }.awaitAll().filterNotNull()
        }

        val resolved = repoListings.flatMap { repoListing ->
            repoListing.listing.items.map {
                ResolvedListingItem(
                    item = it.toListingItem(),
                    repo = repoListing.repo,
                )
            }
        }

        logger.debug("AssetService::getListing resolved listings: {}", resolved)

        val deduped = resolved.distinctBy { it.item.asset }.toSet()

        logger.debug("AssetService::getListing deduped listings: {}", deduped)

        return ResolvedListing(deduped)
    }

    private suspend fun resolveAsset(path: String): AssetResolveResult {
        logger.debug("AssetService::resolveAsset {}", path)

        var resolved: ResolvedAsset? = null
        var assetsFound = false

        for (repo in repos) {
            val asset = repo.getAsset(path) ?: run {
                logger.debug("AssetService::resolveAsset didn't find asset {} in repo {}", path, repo)
                continue
            }

            logger.debug("AssetService::resolveAsset found asset {} in repo {}", path, repo)

            assetsFound = true

            val version = pickAssetVersion(asset, path) ?: run {
                logger.debug("AssetService::resolveAsset no compatible versions found for asset {} in repo {}", asset, repo)
                continue
            }

            resolved = ResolvedAsset(
                path = path,
                version = version,
                repo = repo,
            )

            break
        }

        return when {
            resolved != null -> AssetResolveResult.Success(resolved)
            assetsFound -> AssetResolveResult.NoCompatibleVersionFound
            else -> AssetResolveResult.AssetNotFound
        }
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
    object NoCompatibleVersionFound : AssetFetchResult
    object FileNotFound : AssetFetchResult
}

sealed interface AssetFetchUriResult {
    data class Success(val uri: URI) : AssetFetchUriResult
    object AssetNotFound : AssetFetchUriResult
    object NoCompatibleVersionFound : AssetFetchUriResult
}

private sealed interface AssetResolveResult {
    data class Success(val resolved: ResolvedAsset) : AssetResolveResult
    object AssetNotFound : AssetResolveResult
    object NoCompatibleVersionFound : AssetResolveResult
}

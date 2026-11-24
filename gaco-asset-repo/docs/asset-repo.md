# Asset Repository
The asset repository API solves the problem of providing arbitrary asset files to games in a dynamic way.

**Example**: A game may have multiple maps to play on. 
These maps can be described as assets.
The asset repository acts as a provider and registry of those assets.

## Specification

- each asset repository is just a file tree and must be fully addressable by a plain HTTP index server
- directory listing is not supported, only downloading specific file paths is
- asset repositories contain "assets" as top-level items
- assets are identified by a unique path (e.g. maps/my_game/my_map)
- assets have versions (e.g. simple incrementing int)
- the content of a version of an asset is an arbitrary file tree
- assets versions are listed inside a special `asset.yml` file inside the asset dir
- asset versions can have arbitrary dependency constraints (dependency state providable by the consumer, e.g. app data version / save level data format)
- a special "listing" asset exists, which collects a bunch of assets by their path (the listing is a normal asset itself, also versioned)
- asset repo consumer api provides a way to merge listings from all configured repos (fetch listing asset from every repo, create combined list, dedup by asset path)
- asset repo api provides a way to fetch an asset by path, returning the first matching asset from the first repo that provides it (iteration order = priority) -> Output as blob and URI to blob
- clients using the asset repo api can use a shared config to define which repositories to access, version locks for specific assets and asset exclusions from listings
- asset cache needs to work per repository, instead of per asset because list assets
- version resolution is dynamic and fully handled by the consumer, the asset repository only describes which assets and which versions are available
- to differentiate an asset version directory from a regular asset, asset version directories start with the reserved '@' character
- asset.yml and index.yml are reserved names, which may not be used in asset paths
- version matching: if multiple asset versions match all constraints, the one with the highest version number wins. If both have the same version number, the repository defined first wins

## Example
The following tree describes an asset repository that stores maps for a game called "Spleef".

```mermaid
treeView-beta
maps/
    spleef/             ## listing asset: spleef maps
        arena/          ## map asset
            @1/         ## asset version, arbitrary content
                map.json
                world.tar.xz
            @2/
                map.json
                world.tar.xz
            asset.yml  ## asset version list for "arena"
        snow_valley/    ## map asset
            @1/
                map.json
                world.tar.xz
            asset.yml  ## asset version list for "snow_valley"
        @1/             ## spleef map listing, version 1
            index.yml
        @2/
            index.yml
        asset.yml      ## spleef maps version list
```

There are three assets within this example repo:
1. `maps/spleef` the listing asset describing which maps are available for spleef
2. `maps/spleef/arena` a map called "Arena"
3. `maps/spleef/snow_valley` another map called "Snow Valley"

There are two versions of the "Arena" map and the listing asset.
Asset content can be multiple files, such as `map.yml` + the world save for maps.

The first version of the listing asset may for example only refer to `{maps/spleef/arena}`, while the second version refers to `{maps/spleef/arena, maps/spleef/snow_valley}`.
Note that listings don't include the versions of the referred assets.
Version resolution is dynamic and always done by the consumer, since whether the asset dependencies are fulfilled is also dependent on the consumer.

### Minimal asset.yml
```yml
# maps/spleef/asset.yml
versions: 
  - id: 1
  - id: 2
```
This registers versions 1 and 2 of asset `maps/spleef`.
Versions can be semver strings or integers.

### Version constraints in asset.yml
Most of the time, you'll probably want to impose restrictions on asset versions.
Besides its `id`, each version entry can define additional properties, such as `depends`:

```yml
# maps/spleef/asset.yml
versions:
  - id: 1
    depends:
      minecraft: ">=26.1"
  - id: 2
    depends:
      minecraft: ">=26.3"
```

If the consumer is on Minecraft 26.1 or 26.2, requesting asset `maps/spleef` will return version 1.
In Minecraft 26.3 and later, it will return version 2 (maybe version 2 uses some feature that is only available from 26.3 onwards).
Consumers on Minecraft versions before 26.1 will receive an error if they request it.

## Minimal index.yml
Listing assets require `index.yml`:

```yml
# maps/spleef/@2/index.yml
items:
  - asset: maps/spleef/arena
  - asset: maps/spleef/snow_valley
```

## Properties in index.yml
You can define custom properties for assets in collections.
This can be useful if you have a lot of assets in a list and don't want fetch each individual one to read information from it.
For example, games could read a custom name property for maps, in order to display them in a map list or similar:

```yml
# maps/spleef/@2/index.yml
items:
  - asset: maps/spleef/arena
    properties:
      icon: minecraft:spruce_planks
      authors: ["@person.lclp"]
      name:
        en: Arena
        de: Spleef-Arena

  - asset: maps/spleef/snow_valley
    properties:
      icon: minecraft:snow_block
      authors: 
        - "@person.bops"
        - "@person.lclp"
      name:
        en: Snow Valley
        de: Schneetal
```

## Caching
Assets may be cached locally. 
This is useful to prevent re-fetches from remote repos.

Individual artifact versions are quasi immutable, so the actual files belonging to an asset version can be cached indefinitely.

The artifact version list files `asset.yml` are mutable, so they should be cached for short durations, e.g. TTL of 1h.

Repositories accessed via HTTP can optionally add Cache-Control HTTP headers in order to control the TTL and caching strategy.
Clients are free to utilize or ignore these headers.

# PackDev Plugin

The PackDev plugin can build forge modpacks using ForgeGradle.

### Apply the plugin

```groovy
apply plugin: 'org.moddingx.modgradle.packdev'
```

PackDev will load information on your modlist from a file name `modlist.json` in your project directory. That file can be generated using the [Moonstone IntelliJ plugin](https://github.com/ModdingX/Moonstone).

PackDevs supports both CurseForge and Modrinth as a platform used in the modlist.

### Configure your build

```groovy
minecraft {
    mappings channel: 'official', version: '1.18.2'
}

dependencies {
    minecraft 'net.minecraftforge:forge:1.18.2-40.1.68'
}

modpack {
    author 'test'
    targets {
        curse(12345)
        modrinth()
        server()
        multimc()
    }
}
```

PackDev will handle all ForgeGradle configuration except for setting the mappings. For example it will create run configs for your modpack on its own.

Most of the modpack configuration happens in the `modpack` block.

There you can set a modpack author, which is optional.

Also you can configure targets, that should be built for the modpack. PackDev supports 4 targets:

  * `curse(projectId)`: Builds a CurseForge modpack. If the modpack does not use the `curseforge` platform, resolve the files from the CurseForge API using fingerprints.
  * `modrinth()`: Builds a Modrinth modpack. If the modpack does not use the `modrinth` platform, resolve the files from the Modrinth API using hashes.
  * `server()`: Builds a server zip with an install script and a Dockerfile.
  * `multimc()`: Builds a MultiMC instance zip that includes all the mods directly.

### Modpack Data

Modpack data should go in `data/side` where `side` is the side on which that data should be applied. One of `common`, `client` and `server`. If the same file is found in `common` and aone of the side specific dirs, the one from `client` or `server` precedes over the common one.

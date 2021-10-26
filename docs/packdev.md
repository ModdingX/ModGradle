# PackDev plugin

The PackDev plugin can be used to build modpacks. It is meant to be used together with [Moonstone](https://github.com/noeppi-noeppi/Moonstone). For an example, see [here](https://github.com/SaphieNyako/Core-2-Feywild-).

### Apply the plugin

```groovy
apply plugin: 'io.github.noeppi_noeppi.tools.modgradle.packdev'
```

### Configure the plugin

PackDev is configured using the `modpack` block:

```groovy
modpack {
    projectId 275351
}
```

It must contain the project id. It can also contain definitions for editions:

```groovy
modpack {
    edition 'some_edition'
}
```

For each edition another file is built. Each edition can specify additional overrides. This for example makes it possible to build a skyblock and a non-skyblock pack.

The mod list is retrieved from a file names `modlist.json` in the project folder. It is recommended to use [Moonstone](https://github.com/noeppi-noeppi/Moonstone) for this.

### MultiMC support

MultiMC support can be enabled like this:

```groovy
modpack {
    multimc {
        path '/path/to/multimc/installation'
        instance 'InstanceName'
    }
}
```

By running `gradle multimc` a MultiMC instance will be created for the pack. Whenever it is started, it will copy all overrides from the project into the multimc instance folder. After the game stopped, it will copy all modified overrides back, that have been in the overrides before back.
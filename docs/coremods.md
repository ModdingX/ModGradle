# CoreMods plugin

The CoreMods plugin allows to build coremods written in [TypeScript](https://www.typescriptlang.org/). It requires [node](https://nodejs.org/en/) and [npm](https://www.npmjs.com/).

### Apply the plugin

```groovy
apply plugin: 'io.github.noeppi_noeppi.tools.modgradle.coremods'
```

### Use the plugin

Specify the [CoreModTypes](https://github.com/noeppi-noeppi/CoreModTypes) dependency:

```groovy
repositories {
    maven { url = 'file:///home/tux/dev/util/MinecraftUtilities/maven/' }
}

dependencies {
    coremods 'io.github.noeppi_noeppi.misc:CoreModTypes:5.0.1-1'
}
```

Place your core mods in `src/coremods`. You can import the coremod types from a package named `coremods`.

When building, the core mods will get compiled and a `META-INF/coremods.json` will be created.

The plugin will also create the files `src/coremods/coremods.d.ts` and `src/coremods/tsconfig.json`. These exist for code completion in IDEs. You should gitignore them.


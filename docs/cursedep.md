# CurseDep plugin

The CurseDep plugin allows to easily add dependencies for [CurseForge](https://www.curseforge.com/minecraft/mc-mods) mods.
If the [ForgeGradle](https://github.com/MinecraftForge/ForgeGradle) userdev plugin is applied, the dependencies will also go through `fg.deobf`.

### Apply the plugin

```groovy
apply plugin: 'io.github.noeppi_noeppi.tools.modgradle.cursedep'
```

### Use the plugin

```groovy
dependencies {
    implementation curse.mod(412525, 3491635)
    implementation curse.pack(275351, 3398161, ['aiot-botania', 395617])
}
```

The first line will depend on [LibX](https://www.curseforge.com/minecraft/mc-mods/libx/files/3491635).

The second line will depend on all mods from [Garden of Glass (Questbook Edition)](https://www.curseforge.com/minecraft/modpacks/garden-of-glass-questbook-edition/files/3398161) except for [AIOT Botania](https://www.curseforge.com/minecraft/mc-mods/aiot-botania) and [Botanical Machinery](https://www.curseforge.com/minecraft/mc-mods/botanical-machinery) (project id `395617`).

The first integer is the project id, the second integer is the file id. CurseDep relies on [CurseMaven](https://www.cursemaven.com/).
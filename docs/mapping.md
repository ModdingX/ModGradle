# Mapping plugin

The mapping plugin adds some more mapping channels to [ForgeGradle](https://github.com/MinecraftForge/ForgeGradle).

### Apply the plugin

```groovy
apply plugin: 'org.moddingx.modgradle.mapping'
```

### Channels

  * `none`: A channel that provides no mappings at all. This causes minecraft to run with SRG names. The mapping version used with this channel is ignored.
  * `unofficial`: A channel for the mcp unofficial mappings [here](https://github.com/noeppi-noeppi/MappingUtilities/tree/master/mcp_unofficial).
  * `sugarcane`: [SugarCane](https://github.com/ModdingX/SugarCane) mappings. The mapping version follows the same format as it does for [parchment](https://github.com/ParchmentMC/Librarian/blob/dev/docs/FORGEGRADLE.md). The `sugarcane` channel is only available when [Librarian](https://github.com/ParchmentMC/Librarian) is present.

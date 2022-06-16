# Mapping plugin

The mapping plugin adds some more mapping channels to [ForgeGradle](https://github.com/MinecraftForge/ForgeGradle).

### Apply the plugin

```groovy
apply plugin: 'org.moddingx.modgradle.mapping'
```

### Channels

  * `unofficial`: A channel for the mcp unofficial mappings [here](https://github.com/noeppi-noeppi/MappingUtilities/tree/master/mcp_unofficial).
  * `stable2`, `snapshot2`, `unofficial2`: Same as `stable`, `snapshot` and `unofficial` but the SRG names in there are renamed to the new RGS names, so they can be used in 1.17+
  * `sugarcane`: [SugarCane](https://github.com/noeppi-noeppi/SugarCane) mappings. The mapping version follows the same format as it does for [parchment](https://github.com/ParchmentMC/Librarian/blob/dev/docs/FORGEGRADLE.md). The `sugarcane` channel is only available when [Librarian](https://github.com/ParchmentMC/Librarian) is present.
# JavaDoc plugin

The javadoc plugin handles javadoc links for mods. It uses the javadoc link files from [noeppi-noeppi/Minecraftutilities](https://github.com/noeppi-noeppi/MinecraftUtilities/tree/master/javadoc_links)

### Apply the plugin

```groovy
apply plugin: 'io.github.noeppi_noeppi.tools.modgradle.javadoc'
```

### Tasks

For each task with type `Javadoc` a task named `<name>_link` is created where `<name>` is the name of the javadoc task. The javadoc task will depend on that task. These tasks have the following properties:

  * `config`: A [URL](https://docs.oracle.com/en/java/javase/16/docs/api/java.base/java/net/URL.html) to a json that contains the javadoc links.
  * `includeMinecraft`: A `boolean` on whether to apply the `minecraft` list. See [here](https://github.com/noeppi-noeppi/MinecraftUtilities/tree/master/javadoc_links)
  * `includeMcp`: A `boolean` on whether to apply the `mcp` list. See [here](https://github.com/noeppi-noeppi/MinecraftUtilities/tree/master/javadoc_links)
  * `includeForge`: A `boolean` on whether to apply the `forge` list. See [here](https://github.com/noeppi-noeppi/MinecraftUtilities/tree/master/javadoc_links)

In most cases everything should be inferred for your needs, so you don't need to configure anything manually.
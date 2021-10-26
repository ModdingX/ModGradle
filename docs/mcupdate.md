# McUpdate plugin

The McUpdate plugin makes porting a mod easier. Currently, it contains utilities to update from `1.16.5` to `1.17`.

### Apply the plugin

```groovy
apply plugin: 'io.github.noeppi_noeppi.tools.modgradle.mcupdate'
```

### Use the plugin

If you want to use this, first run the tasks from this plugin before updating the mappings or the forge version or anything.

To use this, you'll first need to set the old mappings like this:

```groovy
mcupdateMappings {
    mappings 'snapshot_1.16.5-20210309'
}
```

Now you can simply run the `mcupdate` task. This will do the following steps:

  * Remap the source code of the mod from old mappings to new official mappings.
  * Scan the source code for occurrences of SRG names in the old format and remap them to the new SRG format.
  * Remap the accesstransformer file
  * Set the `pack_format` to `7` in `pack.mcmeta`.
  * If there's a `Jenkinsfile`, add `jdk 'java16'` to it.

### Additional mappings

When remapping the source code, you can give additional mappings that should be applied as well. These can be for libraries that moved and/or renamed things. By default, the additional mapping file by [LibX](https://github.com/noeppi-noeppi/LibX/blob/1.17/mcupdate.csrg) is applied. To add another file, use

```groovy
mcupdateMappings {
    additionalSrg 'https://url.to.the/srg/file'
}
```
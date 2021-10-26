# MergeArtifact plugin

The MergeArtifact plugin allows to merge some maven artifacts into a mod. It will take coare, that all service files are merged and the files correctly appear in source and javadoc jars.

### Apply the plugin

```groovy
apply plugin: 'io.github.noeppi_noeppi.tools.modgradle.mergeartifact'
```

### Specify an artifact to merge

```groovy
mergeArtifacts {
    include "group:Artifact:version"
}
```

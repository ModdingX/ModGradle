# Jigsaw plugin

The jigsaw plugin makes creating module descriptors more easily.

### Apply the plugin

```groovy
apply plugin: 'io.github.noeppi_noeppi.tools.modgradle.jigsaw'
```

### Tasks

The plugin adds a task called `createModuleDescriptor`. That task will look for a file named `module` in your main source set. It will then read that file and create a module descriptor into the generated sources folder.

The module file format is described [here](https://bitbucket.org/noeppi_noeppi/modulegenerator/wiki/Home) but it does not allow for the `offers` or `offered` keyword.

The `createModuleDescriptor` task has the following properties:

  * `sources`: A [FileCollection](https://docs.gradle.org/current/javadoc/org/gradle/api/file/FileCollection.html) that hold all the source directories to scan for packages.
  * `input`: A [RegularFile](https://docs.gradle.org/current/javadoc/org/gradle/api/file/RegularFile.html) that holds the input file where the module descriptor is defined.
  * `output`: A [RegularFile](https://docs.gradle.org/current/javadoc/org/gradle/api/file/RegularFile.html) that holds the output file for the generated module descriptor.
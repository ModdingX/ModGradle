# ModGradle

ModGradle adds some gradle plugins useful for creating forge mods.

To add it to your gradle buildscript, use

```groovy
buildscript {
    repositories {
        maven { url 'https://maven.minecraftforge.net/' }
        maven { url 'https://maven.moddingx.org/' }
        mavenCentral()
    }

    dependencies {
        classpath 'org.moddingx:ModGradle:<version>'
    }
}
```

More information on the different plugins added by ModGradle:

  * [CoreMods](docs/coremods.md)
  * [Javadoc](docs/javadoc.md)
  * [Mapping](docs/mapping.md)
  * [Meta](docs/meta.md)
  * [PackDev](docs/packdev.md)
  * [SourceJar](docs/sourcejar.md)

package io.github.noeppi_noeppi.tools.modgradle.plugins.mapping;

import io.github.noeppi_noeppi.tools.modgradle.ModGradle;
import io.github.noeppi_noeppi.tools.modgradle.plugins.mapping.provider.NoneProvider;
import io.github.noeppi_noeppi.tools.modgradle.plugins.mapping.provider.SugarcaneProvider;
import io.github.noeppi_noeppi.tools.modgradle.plugins.mapping.provider.UnofficialProvider;
import net.minecraftforge.gradle.mcp.ChannelProvidersExtension;
import org.gradle.api.Plugin;
import org.gradle.api.Project;

import javax.annotation.Nonnull;

// Inject custom mappings and documentation into ForgeGradle
public class MappingPlugin implements Plugin<Project> {

    @Override
    public void apply(@Nonnull Project project) {
        ModGradle.initialiseProject(project);
        ChannelProvidersExtension ext = project.getExtensions().getByType(ChannelProvidersExtension.class);
        ext.addProvider(NoneProvider.INSTANCE);
        ext.addProvider(UnofficialProvider.INSTANCE);
        try {
            Class.forName("org.parchmentmc.librarian.forgegradle.LibrarianForgeGradlePlugin");
            //noinspection TrivialFunctionalExpressionUsage
            ((Runnable) () -> {
                // Maven for SugarCane
                project.getRepositories().maven(r -> {
                    r.setUrl("https://maven.melanx.de/");
                    r.content(c -> c.includeGroup("io.github.noeppi_noeppi.sugarcane"));
                });
                ext.addProvider(SugarcaneProvider.INSTANCE);
            }).run();
        } catch (ClassNotFoundException e) {
            //
        }
    }
}

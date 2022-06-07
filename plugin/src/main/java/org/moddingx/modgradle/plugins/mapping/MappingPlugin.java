package org.moddingx.modgradle.plugins.mapping;

import net.minecraftforge.gradle.mcp.ChannelProvidersExtension;
import org.gradle.api.Plugin;
import org.gradle.api.Project;
import org.moddingx.modgradle.ModGradle;
import org.moddingx.modgradle.plugins.mapping.provider.NoneProvider;
import org.moddingx.modgradle.plugins.mapping.provider.SugarcaneProvider;
import org.moddingx.modgradle.plugins.mapping.provider.UnofficialProvider;

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
                // Maven for SugarCane is added through ModGradle.initialiseProject
                ext.addProvider(SugarcaneProvider.INSTANCE);
            }).run();
        } catch (ClassNotFoundException e) {
            //
        }
    }
}

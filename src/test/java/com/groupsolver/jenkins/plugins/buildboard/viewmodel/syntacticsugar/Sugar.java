package com.groupsolver.jenkins.plugins.buildboard.viewmodel.syntacticsugar;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.groupsolver.jenkins.plugins.buildboard.Config;
import com.groupsolver.jenkins.plugins.buildboard.facade.RelativeLocation;
import com.groupsolver.jenkins.plugins.buildboard.viewmodel.features.HasBadgesBadgePlugin;
import com.groupsolver.jenkins.plugins.buildboard.viewmodel.features.HasJunitRealtime;
import java.util.function.Supplier;

public class Sugar {

    public static JobViewRecipe jobView() {
        return new JobViewRecipe();
    }

    public static JobStateRecipe job() {
        return new JobStateRecipe();
    }

    public static BuildStateRecipe build() {
        return new BuildStateRecipe();
    }

    public static HasBadgesBadgePlugin hasBadgePluginBadges(Config config) {
        return new HasBadgesBadgePlugin(config);
    }

    public static HasBadgesBadgePlugin hasBadgePluginBadges(ConfigStateRecipe config) {
        return new HasBadgesBadgePlugin(config.get());
    }

    public static BadgeBadgePluginRecipe badgePluginBadge() {
        return new BadgeBadgePluginRecipe();
    }

    public static HasJunitRealtime hasJunitRealtime() {
        return new HasJunitRealtime();
    }

    public static RealtimeTestPluginRecipe realtimeTest() {
        return new RealtimeTestPluginRecipe();
    }

    public static <X> X a(Supplier<X> recipe) {
        return recipe.get();
    }

    public static <X> X with(Supplier<X> recipe) {
        return recipe.get();
    }

    // Recipes needed as these get more complex..

    public static RelativeLocation locatedAt(String url) {

        RelativeLocation location = mock(RelativeLocation.class);
        when(location.url()).thenReturn(url);

        return location;
    }

    public static Config withDefaultConfig() {
        return new ConfigStateRecipe().get();
    }

    public static ConfigStateRecipe withConfig() {
        return new ConfigStateRecipe();
    }
}

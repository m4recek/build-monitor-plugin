package com.groupsolver.jenkins.plugins.buildboard;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import hudson.model.View;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;
import org.jvnet.hudson.test.JenkinsRule;
import org.jvnet.hudson.test.junit.jupiter.WithJenkins;

@WithJenkins
class PluginCoexistenceIT {
    @Test
    void loadsBothPluginsAndPersistsIndependentViewTypes(JenkinsRule j) throws Exception {
        Path officialArchive = Path.of(System.getProperty("officialBuildMonitorArchive"));
        j.jenkins.getPluginManager().dynamicLoad(officialArchive.toFile());
        var officialPlugin = j.jenkins.getPluginManager().getPlugin("build-monitor-plugin");
        var boardPlugin = j.jenkins.getPluginManager().getPlugin("groupsolver-build-board");
        assertNotNull(officialPlugin);
        assertNotNull(boardPlugin);
        assertNotEquals(officialPlugin.classLoader, boardPlugin.classLoader);

        Class<? extends View> officialType = officialPlugin
                .classLoader
                .loadClass("com.smartcodeltd.jenkinsci.plugins.buildmonitor.BuildMonitorView")
                .asSubclass(View.class);
        View original = officialType.getConstructor(String.class, String.class).newInstance("Original", "Original");
        BuildBoardView preview = new BuildBoardView("Preview", "Preview");
        j.jenkins.addView(original);
        j.jenkins.addView(preview);
        j.configRoundtrip(original);
        j.configRoundtrip(preview);
        j.jenkins.reload();
        assertEquals(officialType, j.jenkins.getView("Original").getClass());
        assertEquals(BuildBoardView.class, j.jenkins.getView("Preview").getClass());
        assertNotEquals(
                original.getDescriptor().getId(), preview.getDescriptor().getId());
        assertEquals("GroupSolver Build Board", preview.getDescriptor().getDisplayName());
    }
}

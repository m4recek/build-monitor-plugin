package com.groupsolver.jenkins.plugins.buildboard;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;
import org.jvnet.hudson.test.JenkinsRule;
import org.jvnet.hudson.test.junit.jupiter.WithJenkins;

@WithJenkins
class BoardConfigTest {
    @Test
    void persistsBoardSettingsThroughTheConfigurationForm(JenkinsRule j) throws Exception {
        BuildBoardView view = new BuildBoardView("TV", "TV");
        assertEquals(6, view.getConfig().getMaxColumns());
        assertEquals(6, view.getConfig().getMaxRows());
        assertEquals(0.5, view.getConfig().getTextScale());
        view.getConfig().setGroupRegex("_(PROD|DEV)_");
        String stripRegex = "^(?:Tests\\s*(?:»|>>|/)\\s*)?TEST_(?:PROD|DEV)_";
        view.getConfig().setNameStripRegex(stripRegex);
        j.jenkins.addView(view);

        j.configRoundtrip(view);
        BuildBoardView saved = (BuildBoardView) j.jenkins.getView("TV");
        assertEquals(6, saved.getConfig().getMaxColumns());
        assertEquals(0.5, saved.getConfig().getTextScale());
        assertEquals(6, saved.getConfig().getMaxRows());
        assertEquals("_(PROD|DEV)_", saved.getConfig().getGroupRegex());
        assertEquals(stripRegex, saved.getConfig().getNameStripRegex());
        j.jenkins.reload();
        BuildBoardView reloaded = (BuildBoardView) j.jenkins.getView("TV");
        assertEquals(6, reloaded.getConfig().getMaxRows());
        assertEquals(0.5, reloaded.getConfig().getTextScale());
        assertEquals("_(PROD|DEV)_", reloaded.getConfig().getGroupRegex());
        assertEquals(stripRegex, reloaded.getConfig().getNameStripRegex());
    }

    @Test
    void persistsAutomaticRowSizing(JenkinsRule j) throws Exception {
        BuildBoardView view = new BuildBoardView("Automatic", "Automatic");
        view.getConfig().setMaxRows(null);
        j.jenkins.addView(view);
        j.configRoundtrip(view);
        assertNull(view.getConfig().getMaxRows());
    }

    @Test
    void rejectsInvalidDimensions() {
        Config config = new Config();
        assertThrows(IllegalArgumentException.class, () -> config.setMaxRows(0));
        assertThrows(IllegalArgumentException.class, () -> config.setMaxRows(-1));
        assertThrows(IllegalArgumentException.class, () -> config.setMaxColumns(0));
    }
}

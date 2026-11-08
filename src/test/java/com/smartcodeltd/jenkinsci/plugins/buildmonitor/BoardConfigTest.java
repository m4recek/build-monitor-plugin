package com.smartcodeltd.jenkinsci.plugins.buildmonitor;

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
        BuildMonitorView view = new BuildMonitorView("TV", "TV");
        view.getConfig().setMaxColumns(4);
        view.getConfig().setMaxRows(3);
        view.getConfig().setGroupRegex("_(PROD|DEV)_");
        String stripRegex = "^(?:Tests\\s*(?:»|>>|/)\\s*)?TEST_(?:PROD|DEV)_";
        view.getConfig().setNameStripRegex(stripRegex);
        j.jenkins.addView(view);

        j.configRoundtrip(view);
        BuildMonitorView saved = (BuildMonitorView) j.jenkins.getView("TV");
        assertEquals(4, saved.getConfig().getMaxColumns());
        assertEquals(3, saved.getConfig().getMaxRows());
        assertEquals("_(PROD|DEV)_", saved.getConfig().getGroupRegex());
        assertEquals(stripRegex, saved.getConfig().getNameStripRegex());
        j.jenkins.reload();
        BuildMonitorView reloaded = (BuildMonitorView) j.jenkins.getView("TV");
        assertEquals(3, reloaded.getConfig().getMaxRows());
        assertEquals("_(PROD|DEV)_", reloaded.getConfig().getGroupRegex());
        assertEquals(stripRegex, reloaded.getConfig().getNameStripRegex());
    }

    @Test
    void persistsAutomaticRowSizing(JenkinsRule j) throws Exception {
        BuildMonitorView view = new BuildMonitorView("Automatic", "Automatic");
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

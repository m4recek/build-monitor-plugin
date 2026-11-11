package com.groupsolver.jenkins.plugins.buildboard.e2e.config;

import com.microsoft.playwright.junit.Options;
import com.microsoft.playwright.junit.OptionsFactory;

public class PlaywrightConfig implements OptionsFactory {

    @Override
    public Options getOptions() {
        return new Options().setBrowserName("chromium");
    }
}

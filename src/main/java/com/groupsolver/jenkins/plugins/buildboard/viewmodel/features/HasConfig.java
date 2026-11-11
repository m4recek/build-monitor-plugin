package com.groupsolver.jenkins.plugins.buildboard.viewmodel.features;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.groupsolver.jenkins.plugins.buildboard.viewmodel.JobView;

public class HasConfig implements Feature<HasConfig.Config> {
    private final com.groupsolver.jenkins.plugins.buildboard.Config config;

    public HasConfig(com.groupsolver.jenkins.plugins.buildboard.Config config) {
        this.config = config;
    }

    @Override
    public HasConfig of(JobView jobView) {
        return this;
    }

    @Override
    public Config asJson() {
        return new Config(config);
    }

    public static class Config {
        private final com.groupsolver.jenkins.plugins.buildboard.Config config;

        public Config(com.groupsolver.jenkins.plugins.buildboard.Config config) {
            this.config = config;
        }

        @JsonProperty
        public final String displayBadges() {
            return config.getDisplayBadges().name();
        }
    }
}

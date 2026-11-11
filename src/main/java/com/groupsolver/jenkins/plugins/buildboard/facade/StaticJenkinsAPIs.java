package com.groupsolver.jenkins.plugins.buildboard.facade;

import jenkins.model.Jenkins;

public class StaticJenkinsAPIs {

    public boolean hasPlugin(String pluginName) {
        return Jenkins.get().getPlugin(pluginName) != null;
    }
}

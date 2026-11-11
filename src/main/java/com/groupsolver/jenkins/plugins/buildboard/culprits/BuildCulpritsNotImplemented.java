package com.groupsolver.jenkins.plugins.buildboard.culprits;

import hudson.model.Run;
import java.util.Set;

public class BuildCulpritsNotImplemented extends BuildCulpritsRetriever {

    BuildCulpritsNotImplemented() {}

    @Override
    public Set<String> getCulprits(Run<?, ?> run) {
        return Set.of();
    }

    @Override
    protected Set<String> getCommittersForRun(Run<?, ?> run) {
        return Set.of();
    }
}

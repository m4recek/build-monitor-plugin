package com.groupsolver.jenkins.plugins.buildboard.build;

import com.groupsolver.jenkins.plugins.buildboard.viewmodel.BuildViewModel;
import com.groupsolver.jenkins.plugins.buildboard.viewmodel.JobView;
import hudson.Extension;
import hudson.model.Descriptor;
import org.kohsuke.stapler.DataBoundConstructor;

public class GetLastBuild implements GetBuildViewModel {

    @Override
    public BuildViewModel from(JobView job) {
        return job.lastBuild();
    }

    @DataBoundConstructor
    public GetLastBuild() {}

    @Extension
    public static class GetLastCompletedBuildDescriptor extends Descriptor<GetBuildViewModel> {}
}

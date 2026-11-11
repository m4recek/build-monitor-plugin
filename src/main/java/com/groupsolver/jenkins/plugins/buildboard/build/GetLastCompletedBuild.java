package com.groupsolver.jenkins.plugins.buildboard.build;

import com.groupsolver.jenkins.plugins.buildboard.viewmodel.BuildViewModel;
import com.groupsolver.jenkins.plugins.buildboard.viewmodel.JobView;
import hudson.Extension;
import hudson.model.Descriptor;
import org.kohsuke.stapler.DataBoundConstructor;

public class GetLastCompletedBuild implements GetBuildViewModel {

    @Override
    public BuildViewModel from(JobView job) {
        return job.lastCompletedBuild();
    }

    @DataBoundConstructor
    public GetLastCompletedBuild() {}

    @Extension
    public static class GetLastCompletedBuildDescriptor extends Descriptor<GetBuildViewModel> {}
}

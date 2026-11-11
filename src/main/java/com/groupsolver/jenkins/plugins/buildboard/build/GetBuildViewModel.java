package com.groupsolver.jenkins.plugins.buildboard.build;

import com.groupsolver.jenkins.plugins.buildboard.viewmodel.BuildViewModel;
import com.groupsolver.jenkins.plugins.buildboard.viewmodel.JobView;
import hudson.model.Describable;
import hudson.model.Descriptor;
import jenkins.model.Jenkins;

public interface GetBuildViewModel extends Describable<GetBuildViewModel> {

    BuildViewModel from(JobView job);

    @Override
    public default Descriptor<GetBuildViewModel> getDescriptor() {
        return Jenkins.get().getDescriptor(this.getClass());
    }
}

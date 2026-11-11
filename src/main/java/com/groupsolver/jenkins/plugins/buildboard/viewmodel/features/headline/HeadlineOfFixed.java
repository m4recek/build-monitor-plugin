package com.groupsolver.jenkins.plugins.buildboard.viewmodel.features.headline;

import com.groupsolver.jenkins.plugins.buildboard.readability.Lister;
import com.groupsolver.jenkins.plugins.buildboard.viewmodel.BuildViewModel;
import com.groupsolver.jenkins.plugins.buildboard.viewmodel.JobView;
import hudson.model.Result;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.Set;

public class HeadlineOfFixed implements CandidateHeadline {

    private final JobView job;
    private final HeadlineConfig config;

    public HeadlineOfFixed(JobView job, HeadlineConfig config) {
        this.job = job;
        this.config = config;
    }

    @Override
    public boolean isApplicableTo(JobView job) {
        return didTheJobJustGetFixedWith(job.lastCompletedBuild());
    }

    @Override
    public Headline asJson() {
        return new Headline(textFor(job.lastCompletedBuild()));
    }

    private String textFor(BuildViewModel lastBuild) {

        return Lister.describe(
                "Back in the green!",
                "Fixed after %s committed their changes :-)",
                new LinkedList<>(committersOf(lastBuild)));
    }

    private boolean didTheJobJustGetFixedWith(BuildViewModel build) {
        return Result.SUCCESS.equals(build.result()) && previousFailed(build);
    }

    private boolean previousFailed(BuildViewModel build) {
        return build.hasPreviousBuild()
                && (Result.FAILURE.equals(build.previousBuild().result())
                        || Result.UNSTABLE.equals(build.previousBuild().result())
                        || Result.ABORTED.equals(build.previousBuild().result()));
    }

    private Set<String> committersOf(BuildViewModel build) {
        return config.displayCommitters ? build.committers() : new HashSet<>();
    }
}

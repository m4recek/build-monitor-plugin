package com.groupsolver.jenkins.plugins.buildboard.viewmodel.features.headline;

import com.groupsolver.jenkins.plugins.buildboard.viewmodel.JobView;

/**
 * @author Jan Molak
 */
public class NoHeadline implements CandidateHeadline {

    @Override
    public boolean isApplicableTo(JobView job) {
        return true;
    }

    @Override
    public Headline asJson() {
        return new Headline("");
    }
}

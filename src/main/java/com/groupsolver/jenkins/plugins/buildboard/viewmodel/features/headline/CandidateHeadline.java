package com.groupsolver.jenkins.plugins.buildboard.viewmodel.features.headline;

import com.groupsolver.jenkins.plugins.buildboard.viewmodel.JobView;
import com.groupsolver.jenkins.plugins.buildboard.viewmodel.features.SerialisableAsJsonObjectCalled;

/**
 * @author Jan Molak
 */
public interface CandidateHeadline extends SerialisableAsJsonObjectCalled<Headline> {
    boolean isApplicableTo(JobView job);
}

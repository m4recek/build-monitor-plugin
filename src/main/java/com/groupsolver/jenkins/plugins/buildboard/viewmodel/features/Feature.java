package com.groupsolver.jenkins.plugins.buildboard.viewmodel.features;

import com.groupsolver.jenkins.plugins.buildboard.viewmodel.JobView;

/**
 * @author Jan Molak
 */
public interface Feature<JSON> extends SerialisableAsJsonObjectCalled<JSON> {
    <F extends Feature> F of(JobView jobView);
}

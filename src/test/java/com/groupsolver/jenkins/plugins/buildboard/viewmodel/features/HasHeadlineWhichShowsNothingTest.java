package com.groupsolver.jenkins.plugins.buildboard.viewmodel.features;

import static com.groupsolver.jenkins.plugins.buildboard.viewmodel.syntacticsugar.Sugar.a;
import static com.groupsolver.jenkins.plugins.buildboard.viewmodel.syntacticsugar.Sugar.job;
import static com.groupsolver.jenkins.plugins.buildboard.viewmodel.syntacticsugar.Sugar.jobView;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.emptyString;
import static org.hamcrest.Matchers.is;

import com.groupsolver.jenkins.plugins.buildboard.viewmodel.JobView;
import com.groupsolver.jenkins.plugins.buildboard.viewmodel.features.headline.HeadlineConfig;
import org.junit.jupiter.api.Test;

class HasHeadlineWhichShowsNothingTest {

    private JobView view;

    @Test
    void should_say_nothing_if_no_builds_were_executed() {
        view = a(jobView().which(hasHeadlineThatShowsCommitters()).of(a(job().thatHasNeverRun())));

        assertThat(headlineOf(view), is(emptyString()));
    }

    // --

    private Feature hasHeadlineThatShowsCommitters() {
        return new HasHeadline(new HeadlineConfig(true));
    }

    private String headlineOf(JobView job) {
        return job.which(HasHeadline.class).asJson().value();
    }
}

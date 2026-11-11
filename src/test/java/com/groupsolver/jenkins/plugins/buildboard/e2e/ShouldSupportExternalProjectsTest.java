package com.groupsolver.jenkins.plugins.buildboard.e2e;

import static com.groupsolver.jenkins.plugins.buildboard.e2e.utils.BuildBoardViewUtils.createBuildBoardView;

import com.groupsolver.jenkins.plugins.buildboard.e2e.config.PlaywrightConfig;
import com.groupsolver.jenkins.plugins.buildboard.e2e.pages.BuildBoardViewPage;
import com.groupsolver.jenkins.plugins.buildboard.e2e.utils.ExternalJobUtils;
import com.microsoft.playwright.Page;
import com.microsoft.playwright.junit.UsePlaywright;
import hudson.model.Result;
import org.junit.jupiter.api.Test;
import org.jvnet.hudson.test.JenkinsRule;
import org.jvnet.hudson.test.junit.jupiter.WithJenkins;

@WithJenkins
@UsePlaywright(PlaywrightConfig.class)
class ShouldSupportExternalProjectsTest {

    @Test
    void test(Page p, JenkinsRule j) {
        var job = ExternalJobUtils.createExternalJob(j, "External job")
                .notifyOf(Result.SUCCESS)
                .get();
        var view = createBuildBoardView(j, "Build Monitor").addJobs(job);

        BuildBoardViewPage.from(p, view).goTo().getJob(job.getDisplayName()).hasStatus(Result.SUCCESS);
    }
}

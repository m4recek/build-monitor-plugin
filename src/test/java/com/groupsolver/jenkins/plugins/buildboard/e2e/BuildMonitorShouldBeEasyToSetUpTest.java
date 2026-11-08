package com.groupsolver.jenkins.plugins.buildboard.e2e;

import static com.groupsolver.jenkins.plugins.buildboard.e2e.utils.BuildBoardViewUtils.createBuildBoardView;
import static com.groupsolver.jenkins.plugins.buildboard.e2e.utils.FreeStyleProjectUtils.createFreeStyleProject;

import com.groupsolver.jenkins.plugins.buildboard.e2e.config.PlaywrightConfig;
import com.groupsolver.jenkins.plugins.buildboard.e2e.pages.BuildBoardViewPage;
import com.microsoft.playwright.Page;
import com.microsoft.playwright.junit.UsePlaywright;
import org.junit.jupiter.api.Test;
import org.jvnet.hudson.test.JenkinsRule;
import org.jvnet.hudson.test.junit.jupiter.WithJenkins;

@WithJenkins
@UsePlaywright(PlaywrightConfig.class)
class BuildMonitorShouldBeEasyToSetUpTest {

    @Test
    void test(Page p, JenkinsRule j) {
        var project = createFreeStyleProject(j, "Example job").get();
        var view = createBuildBoardView(j, "Build Monitor").addJobs(project);

        BuildBoardViewPage.from(p, view).goTo().hasJobsCount(1).hasJob(project.getDisplayName());
    }
}

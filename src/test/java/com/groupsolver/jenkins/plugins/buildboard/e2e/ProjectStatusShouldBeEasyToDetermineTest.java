package com.groupsolver.jenkins.plugins.buildboard.e2e;

import static com.groupsolver.jenkins.plugins.buildboard.e2e.utils.BuildBoardViewUtils.createBuildBoardView;
import static com.groupsolver.jenkins.plugins.buildboard.e2e.utils.FreeStyleProjectUtils.createFreeStyleProject;

import com.groupsolver.jenkins.plugins.buildboard.e2e.config.PlaywrightConfig;
import com.groupsolver.jenkins.plugins.buildboard.e2e.pages.BuildBoardViewPage;
import com.microsoft.playwright.Page;
import com.microsoft.playwright.junit.UsePlaywright;
import hudson.model.Result;
import org.junit.jupiter.api.Test;
import org.jvnet.hudson.test.JenkinsRule;
import org.jvnet.hudson.test.junit.jupiter.WithJenkins;

@WithJenkins
@UsePlaywright(PlaywrightConfig.class)
class ProjectStatusShouldBeEasyToDetermineTest {

    @Test
    void visualisingASuccessfulProject(Page p, JenkinsRule j) {
        var project =
                createFreeStyleProject(j, "Successful").run(Result.SUCCESS).getProject();
        var view = createBuildBoardView(j, "Build Monitor").addJobs(project);

        BuildBoardViewPage.from(p, view)
                .goTo()
                .hasJobsCount(1)
                .getJob(project.getDisplayName())
                .hasStatus(Result.SUCCESS);
    }

    @Test
    void visualisingAFailingProject(Page p, JenkinsRule j) {
        var project = createFreeStyleProject(j, "Failing")
                .addTask(new hudson.tasks.Shell("exit 1"))
                .run(Result.FAILURE)
                .getProject();
        var view = createBuildBoardView(j, "Build Monitor").addJobs(project);

        BuildBoardViewPage.from(p, view)
                .goTo()
                .hasJobsCount(1)
                .getJob(project.getDisplayName())
                .hasStatus(Result.FAILURE);
    }
}

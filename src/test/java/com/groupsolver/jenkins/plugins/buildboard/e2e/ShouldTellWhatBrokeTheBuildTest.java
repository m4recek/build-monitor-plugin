package com.groupsolver.jenkins.plugins.buildboard.e2e;

import static com.groupsolver.jenkins.plugins.buildboard.e2e.utils.BuildBoardViewUtils.createBuildBoardView;
import static com.groupsolver.jenkins.plugins.buildboard.e2e.utils.FreeStyleProjectUtils.createFreeStyleProject;

import com.groupsolver.jenkins.plugins.buildboard.e2e.config.PlaywrightConfig;
import com.groupsolver.jenkins.plugins.buildboard.e2e.pages.BuildBoardViewPage;
import com.groupsolver.jenkins.plugins.buildboard.e2e.pages.FailureCauseManagementPage;
import com.microsoft.playwright.Page;
import com.microsoft.playwright.junit.UsePlaywright;
import org.junit.jupiter.api.Test;
import org.jvnet.hudson.test.JenkinsRule;
import org.jvnet.hudson.test.junit.jupiter.WithJenkins;

@WithJenkins
@UsePlaywright(PlaywrightConfig.class)
class ShouldTellWhatBrokeTheBuildTest {

    @Test
    void test(Page p, JenkinsRule j) {
        var project = createFreeStyleProject(j, "Discovery One")
                .addTask(new hudson.tasks.Shell("exit 1"))
                .get();

        FailureCauseManagementPage.from(p, project)
                .goTo()
                .createFailureCause(
                        "Rogue AI", "Pod bay doors didn't open", "Build step 'Execute shell' marked build as failure");

        project.scheduleBuild2(0);
        var view = createBuildBoardView(j, "Build Monitor").displayAllProjects();
        BuildBoardViewPage.from(p, view).goTo().getJob("Discovery One").hasIdentifiedProblem("Rogue AI");
    }
}

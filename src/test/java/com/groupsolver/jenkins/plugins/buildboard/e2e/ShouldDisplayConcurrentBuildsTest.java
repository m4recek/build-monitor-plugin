package com.groupsolver.jenkins.plugins.buildboard.e2e;

import static com.groupsolver.jenkins.plugins.buildboard.e2e.utils.BuildBoardViewUtils.createBuildBoardView;
import static com.groupsolver.jenkins.plugins.buildboard.e2e.utils.PipelineJobUtils.createPipelineJob;

import com.groupsolver.jenkins.plugins.buildboard.e2e.config.PlaywrightConfig;
import com.groupsolver.jenkins.plugins.buildboard.e2e.pages.BuildBoardViewPage;
import com.microsoft.playwright.Page;
import com.microsoft.playwright.junit.UsePlaywright;
import org.junit.jupiter.api.Test;
import org.jvnet.hudson.test.JenkinsRule;
import org.jvnet.hudson.test.junit.jupiter.WithJenkins;

@WithJenkins
@UsePlaywright(PlaywrightConfig.class)
class ShouldDisplayConcurrentBuildsTest {

    @Test
    void test(Page p, JenkinsRule j) {
        createPipelineJob(j, "My Pipeline", "pipelineStage.jenkinsfile").run().run();
        var view = createBuildBoardView(j, "Build Monitor").displayAllProjects();

        BuildBoardViewPage.from(p, view).goTo().getJob("My Pipeline").hasBuilds("#2", "#1");
    }
}

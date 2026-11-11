package com.groupsolver.jenkins.plugins.buildboard.e2e;

import static com.groupsolver.jenkins.plugins.buildboard.e2e.utils.BuildBoardViewUtils.createBuildBoardView;
import static com.groupsolver.jenkins.plugins.buildboard.e2e.utils.PipelineJobUtils.createPipelineJob;

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
class ShouldDisplayBadgesTest {

    @Test
    void test(Page p, JenkinsRule j) {
        createPipelineJob(j, "Example job", "singleStagePipeline.jenkinsfile").run(Result.SUCCESS);
        var view = createBuildBoardView(j, "Build Monitor").displayAllProjects();

        BuildBoardViewPage.from(p, view).goTo().getJob("Example job").hasBadge("Example badge");
    }
}

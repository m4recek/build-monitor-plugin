package com.groupsolver.jenkins.plugins.buildboard.e2e;

import static com.groupsolver.jenkins.plugins.buildboard.e2e.utils.BuildBoardViewUtils.createBuildBoardView;
import static com.groupsolver.jenkins.plugins.buildboard.e2e.utils.FreeStyleProjectUtils.createFreeStyleProject;

import com.groupsolver.jenkins.plugins.buildboard.e2e.config.PlaywrightConfig;
import com.groupsolver.jenkins.plugins.buildboard.e2e.pages.BuildBoardViewPage;
import com.microsoft.playwright.Page;
import com.microsoft.playwright.junit.UsePlaywright;
import hudson.model.FreeStyleProject;
import hudson.model.Result;
import java.util.stream.IntStream;
import org.junit.jupiter.api.Test;
import org.jvnet.hudson.test.JenkinsRule;
import org.jvnet.hudson.test.junit.jupiter.WithJenkins;

@WithJenkins
@UsePlaywright(PlaywrightConfig.class)
class ShouldSnapBetweenPagesTest {

    @Test
    void snapsToTheSecondPageWhenTheViewportIsScrolledPastTheBoundary(Page p, JenkinsRule j) {
        FreeStyleProject[] projects = IntStream.rangeClosed(1, 5)
                .mapToObj(index -> createFreeStyleProject(j, "Job " + index)
                        .run(Result.SUCCESS)
                        .getProject())
                .toArray(FreeStyleProject[]::new);

        var view = createBuildBoardView(j, "Build Monitor")
                .addJobs(projects)
                .withMaximumColumns(2)
                .withTextScale(2.0);

        BuildBoardViewPage.from(p, view)
                .goTo()
                .hasJobsCount(5)
                .hasPageCount(3)
                .scrollPastPageBoundaryTowards(2)
                .hasSnappedToPage(2)
                .hasActivePage(2);
    }
}

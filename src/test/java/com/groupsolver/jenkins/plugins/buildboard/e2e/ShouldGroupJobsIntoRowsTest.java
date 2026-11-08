package com.groupsolver.jenkins.plugins.buildboard.e2e;

import static com.groupsolver.jenkins.plugins.buildboard.e2e.utils.BuildBoardViewUtils.createBuildBoardView;
import static com.groupsolver.jenkins.plugins.buildboard.e2e.utils.FreeStyleProjectUtils.createFreeStyleProject;
import static com.microsoft.playwright.assertions.PlaywrightAssertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;

import com.groupsolver.jenkins.plugins.buildboard.e2e.config.PlaywrightConfig;
import com.groupsolver.jenkins.plugins.buildboard.e2e.pages.BuildBoardViewPage;
import com.microsoft.playwright.Page;
import com.microsoft.playwright.junit.UsePlaywright;
import hudson.model.FreeStyleProject;
import java.util.stream.IntStream;
import org.junit.jupiter.api.Test;
import org.jvnet.hudson.test.JenkinsRule;
import org.jvnet.hudson.test.junit.jupiter.WithJenkins;

@WithJenkins
@UsePlaywright(PlaywrightConfig.class)
class ShouldGroupJobsIntoRowsTest {
    @Test
    void groupsRowsAcrossPagesAndStripsOnlyTileLabels(Page p, JenkinsRule j) {
        p.onPageError(error -> {
            throw new AssertionError(error);
        });
        FreeStyleProject[] projects = IntStream.rangeClosed(1, 5)
                .mapToObj(index ->
                        createFreeStyleProject(j, "TEST_PROD_job_" + index).get())
                .toArray(FreeStyleProject[]::new);
        FreeStyleProject dev =
                createFreeStyleProject(j, "TEST_DEV_graphql_logic").get();
        var view = createBuildBoardView(j, "TV").addJobs(projects).addJobs(dev).withMaximumColumns(2);
        view.get().getConfig().setMaxRows(2);
        view.get().getConfig().setGroupRegex("_(PROD|DEV)_");
        view.get().getConfig().setNameStripRegex("^TEST_(?:PROD|DEV)_");

        BuildBoardViewPage.from(p, view).goTo().hasJobsCount(6).hasPageCount(2);
        var pages = p.locator(".bm-grid-page");
        assertThat(pages.nth(0).locator(".bm-cell")).hasCount(3);
        assertThat(pages.nth(0).locator(".bm-grid-row__group").nth(0)).hasText("DEV");
        assertThat(pages.nth(0).locator(".bm-grid-row__group").nth(1)).hasText("PROD");
        assertThat(pages.nth(0).locator(".bm-grid-row").nth(0).locator(".bm-cell"))
                .hasCount(1);
        assertThat(pages.nth(1).locator(".bm-grid-row")).hasCount(2);
        assertThat(pages.nth(1).locator(".bm-grid-row").nth(0).locator(".bm-cell"))
                .hasCount(2);
        assertThat(pages.nth(1).locator(".bm-grid-row").nth(1).locator(".bm-cell"))
                .hasCount(1);
        assertThat(pages.nth(1).locator(".bm-grid-row__group").nth(0)).hasText("PROD");
        assertThat(pages.nth(1).locator(".bm-grid-row__group").nth(1)).hasText("");
        assertThat(pages.nth(0).locator(".bm-cell h2").nth(0)).hasText("graphql_logic");
        String originalUrl = (String) p.evaluate("""
                async () => {
                  const response = await fetch('fetchJobViews');
                  const { data } = await response.json();
                  return data.find(job => job.name === 'TEST_DEV_graphql_logic').url;
                }
                """);
        assertThat(pages.nth(0).locator(".bm-cell").nth(0)).hasAttribute("href", originalUrl);
        p.locator(".bm-grid-row").evaluateAll("""
                rows => Promise.all(rows.flatMap(row => row.getAnimations()).map(animation => animation.finished))
                """);
        double devX = pages.nth(0).locator(".bm-cell").nth(0).boundingBox().x;
        double prodX = pages.nth(0)
                .locator(".bm-grid-row")
                .nth(1)
                .locator(".bm-cell")
                .nth(0)
                .boundingBox()
                .x;
        assertEquals(devX, prodX, 1.0);
        assertEquals("TEST_DEV_graphql_logic", dev.getName());
    }
}

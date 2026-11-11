package com.groupsolver.jenkins.plugins.buildboard.e2e;

import static com.groupsolver.jenkins.plugins.buildboard.e2e.utils.BuildBoardViewUtils.createBuildBoardView;
import static com.groupsolver.jenkins.plugins.buildboard.e2e.utils.FreeStyleProjectUtils.createFreeStyleProject;
import static com.microsoft.playwright.assertions.PlaywrightAssertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

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
        view.get().getConfig().setGroupRegex("_(?:(PROD)|(DEV))_");
        view.get().getConfig().setNameStripRegex("^TEST_(?:PROD|DEV)_");

        BuildBoardViewPage.from(p, view).goTo().hasJobsCount(6).hasPageCount(2);
        assertTrue(p.locator("script[src*=\"/plugin/groupsolver-build-board/\"]")
                .getAttribute("src")
                .endsWith("?version="
                        + j.jenkins
                                .getPluginManager()
                                .getPlugin("groupsolver-build-board")
                                .getVersion()));
        var pages = p.locator(".bm-grid-page");
        assertThat(pages.nth(0).locator(".bm-cell")).hasCount(4);
        assertThat(pages.nth(0).locator(".bm-grid-row__group").nth(0)).hasText("PROD");
        assertThat(pages.nth(0).locator(".bm-grid-row__group")).hasCount(1);
        assertThat(pages.nth(0).locator(".bm-grid-row").nth(1).locator(".bm-grid-row__group"))
                .hasCount(0);
        assertThat(pages.nth(0).locator(".bm-grid-row").nth(0).locator(".bm-cell"))
                .hasCount(2);
        assertThat(pages.nth(1).locator(".bm-grid-row")).hasCount(2);
        assertThat(pages.nth(1).locator(".bm-grid-row").nth(0).locator(".bm-cell"))
                .hasCount(1);
        assertThat(pages.nth(1).locator(".bm-grid-row").nth(1).locator(".bm-cell"))
                .hasCount(1);
        assertThat(pages.nth(1).locator(".bm-grid-row__group").nth(0)).hasText("PROD");
        assertThat(pages.nth(1).locator(".bm-grid-row__group").nth(1)).hasText("DEV");
        assertThat(pages.nth(1)
                        .locator(".bm-grid-row")
                        .nth(1)
                        .locator(".bm-cell h2")
                        .nth(0))
                .hasText("graphql_logic");
        String originalUrl = (String) p.evaluate("""
                async () => {
                  const response = await fetch('fetchJobViews');
                  const { data } = await response.json();
                  return data.find(job => job.name === 'TEST_DEV_graphql_logic').url;
                }
                """);
        assertThat(pages.nth(1)
                        .locator(".bm-grid-row")
                        .nth(1)
                        .locator(".bm-cell")
                        .nth(0))
                .hasAttribute("href", originalUrl);
        p.locator(".bm-grid-row").evaluateAll("""
                rows => Promise.all(rows.flatMap(row => row.getAnimations()).map(animation => animation.finished))
                """);
        var firstRow = pages.nth(0).locator(".bm-grid-row").nth(0);
        var firstTile = firstRow.locator(".bm-cell").nth(0).boundingBox();
        var nextTile = firstRow.locator(".bm-cell").nth(1).boundingBox();
        var nextRowTile = pages.nth(0)
                .locator(".bm-grid-row")
                .nth(1)
                .locator(".bm-cell")
                .nth(0)
                .boundingBox();
        assertEquals(
                nextTile.x - firstTile.x - firstTile.width,
                nextRowTile.y - firstTile.y - firstTile.height,
                0.5,
                "Continuation rows must use the same tile gap as columns");
        assertEquals(firstTile.height, nextRowTile.height, 0.5, "Group headings must not shrink tile rows");
        assertThat(pages.nth(0).locator(".bm-grid-row__group")).hasCSS("font-size", "16px");
        assertThat(pages.nth(0).locator(".bm-grid-row__group")).hasCSS("line-height", "32px");
        for (var size : new int[][] {{1280, 720}, {1024, 768}}) {
            p.setViewportSize(size[0], size[1]);
            for (var page : pages.all()) {
                var tiles = page.locator(".bm-cell").all();
                double tileHeight = tiles.get(0).boundingBox().height;
                for (var tile : tiles) {
                    assertEquals(tileHeight, tile.boundingBox().height, 0.5, "Every tile row must have equal height");
                }
                for (var label : page.locator(".bm-grid-row__group").all()) {
                    var labelBox = label.boundingBox();
                    var row =
                            label.locator("xpath=following-sibling::section[1]").boundingBox();
                    assertEquals(labelBox.y + labelBox.height, row.y, 0.5, "Headings must sit above tile rows");
                }
            }
        }
        double devX = pages.nth(1)
                .locator(".bm-grid-row")
                .nth(1)
                .locator(".bm-cell")
                .nth(0)
                .boundingBox()
                .x;
        double prodX = pages.nth(1)
                .locator(".bm-grid-row")
                .nth(0)
                .locator(".bm-cell")
                .nth(0)
                .boundingBox()
                .x;
        assertEquals(devX, prodX, 1.0);
        assertEquals("TEST_DEV_graphql_logic", dev.getName());

        var ungroupedView = createBuildBoardView(j, "Ungrouped")
                .addJobs(projects[0], projects[1], projects[2])
                .withMaximumColumns(2);
        ungroupedView.get().getConfig().setMaxRows(3);
        BuildBoardViewPage.from(p, ungroupedView).goTo().hasJobsCount(3).hasPageCount(1);
        assertThat(p.locator(".bm-grid-row__group")).hasCount(0);
        p.locator(".bm-grid-row").evaluateAll("""
                rows => Promise.all(rows.flatMap(row => row.getAnimations()).map(animation => animation.finished))
                """);
        double emptyRowHeight = ((Number) p.locator(".bm-grid").evaluate("""
                grid => {
                  const style = getComputedStyle(grid);
                  const gap = parseFloat(getComputedStyle(grid.querySelector('.bm-grid-row__jobs')).columnGap);
                  return (grid.clientHeight - parseFloat(style.paddingBottom) - 2 * gap) / 3;
                }
                """)).doubleValue();
        for (var tile : p.locator(".bm-cell").all()) {
            assertEquals(
                    emptyRowHeight, tile.boundingBox().height, 0.5, "Configured empty rows must retain their slots");
        }
    }
}

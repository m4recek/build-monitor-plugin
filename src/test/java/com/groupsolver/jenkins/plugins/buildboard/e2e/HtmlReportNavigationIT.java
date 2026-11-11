package com.groupsolver.jenkins.plugins.buildboard.e2e;

import static com.groupsolver.jenkins.plugins.buildboard.e2e.utils.BuildBoardViewUtils.createBuildBoardView;
import static com.microsoft.playwright.assertions.PlaywrightAssertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.cloudbees.hudson.plugins.folder.Folder;
import com.groupsolver.jenkins.plugins.buildboard.e2e.pages.BuildBoardViewPage;
import com.groupsolver.jenkins.plugins.buildboard.viewmodel.duration.HumanReadableDuration;
import com.microsoft.playwright.Locator;
import com.microsoft.playwright.Page;
import com.microsoft.playwright.junit.Options;
import com.microsoft.playwright.junit.OptionsFactory;
import com.microsoft.playwright.junit.UsePlaywright;
import com.microsoft.playwright.options.AriaRole;
import com.microsoft.playwright.options.KeyboardModifier;
import com.microsoft.playwright.options.MouseButton;
import hudson.FilePath;
import hudson.model.Result;
import java.nio.file.Path;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import org.jenkinsci.plugins.workflow.cps.CpsFlowDefinition;
import org.jenkinsci.plugins.workflow.job.WorkflowJob;
import org.junit.jupiter.api.Test;
import org.jvnet.hudson.test.JenkinsRule;
import org.jvnet.hudson.test.junit.jupiter.WithJenkins;

@WithJenkins
@UsePlaywright(HtmlReportNavigationIT.NavigationBrowser.class)
public class HtmlReportNavigationIT {
    public static class NavigationBrowser implements OptionsFactory {
        @Override
        public Options getOptions() {
            return new Options().setBrowserName("chromium").setChannel("chromium");
        }
    }

    @Test
    void opensLatestBuildReportOrConsoleIncludingBuildNumberClicks(Page p, JenkinsRule j) throws Exception {
        j.jenkins
                .getPluginManager()
                .dynamicLoad(Path.of(System.getProperty("htmlPublisherArchive")).toFile());
        Folder folder = j.createProject(Folder.class, "Tests");
        WorkflowJob job = folder.createProject(WorkflowJob.class, "TEST_DEV_html");
        var view = createBuildBoardView(j, "Navigation").displayAllProjects().displayNestedProjectsFromSubfolders();
        var board = BuildBoardViewPage.from(p, view);
        String jobUrl = j.getURL() + "view/Navigation/" + job.getUrl();

        board.goTo().hasJobsCount(1);
        Page neverBuilt = p.context().waitForPage(() -> p.locator(".bm-cell").click());
        assertThat(neverBuilt).hasURL(jobUrl);
        assertThat(p).hasURL(view.get().getAbsoluteUrl());
        neverBuilt.close();

        String publishReport = """
                node {
                  writeFile file: 'report/index.html', text: '<h1>Latest HTML result</h1>'
                  publishHTML(target: [reportName: 'HTML Report', reportDir: 'report', reportFiles: 'index.html',
                    keepAll: true, alwaysLinkToLastBuild: true, allowMissing: false])
                }
                """;
        job.setDefinition(new CpsFlowDefinition(publishReport, true));
        j.buildAndAssertSuccess(job);
        board.goTo().hasJobsCount(1);
        assertThat(p.locator(".bm-cell a")).hasCount(0);
        Page firstReport = p.context()
                .waitForPage(() -> p.locator(".bm-cell")
                        .getByText("#1", new Locator.GetByTextOptions().setExact(true))
                        .click());
        assertThat(firstReport).hasURL(jobUrl + "1/HTML_20Report/index.html");
        assertThat(firstReport.locator("h1")).hasText("Latest HTML result");
        assertThat(p).hasURL(view.get().getAbsoluteUrl());
        firstReport.close();

        job.setDefinition(new CpsFlowDefinition("echo 'No report for this run'", true));
        j.buildAndAssertSuccess(job);
        board.goTo().hasJobsCount(1);
        Page secondConsole = p.context().waitForPage(() -> p.locator(".bm-cell").click());
        assertThat(secondConsole).hasURL(jobUrl + "2/console");
        assertThat(secondConsole.locator(".console-output")).containsText("No report for this run");
        assertThat(p).hasURL(view.get().getAbsoluteUrl());
        secondConsole.close();

        job.setDefinition(new CpsFlowDefinition(publishReport, true));
        var latest = j.buildAndAssertSuccess(job);
        board.goTo().hasJobsCount(1);
        assertThat(p.locator(".bm-cell")).hasAttribute("href", job.getUrl() + "3/HTML_20Report/index.html");
        new FilePath(latest.getRootDir()
                        .toPath()
                        .resolve("htmlreports/HTML_20Report/index.html")
                        .toFile())
                .delete();
        board.goTo().hasJobsCount(1);
        Page missingReport = p.context().waitForPage(() -> p.locator(".bm-cell").click());
        assertThat(missingReport).hasURL(jobUrl + "3/console");
        missingReport.close();

        job.setDefinition(new CpsFlowDefinition("""
                node {
                  writeFile file: 'report/details/test result.html', text: '<h1>Custom report entry</h1>'
                  publishHTML(target: [reportName: 'HTML Report', reportDir: 'report', reportFiles: 'details/*.html',
                    keepAll: true, alwaysLinkToLastBuild: true, allowMissing: false])
                }
                """, true));
        j.buildAndAssertSuccess(job);
        board.goTo().hasJobsCount(1);
        Page customReport = p.context().waitForPage(() -> p.locator(".bm-cell").click());
        assertThat(customReport).hasURL(jobUrl + "4/HTML_20Report/details/test%20result.html");
        assertThat(customReport.locator("h1")).hasText("Custom report entry");
        customReport.close();

        job.setDefinition(new CpsFlowDefinition("echo 'Recent console run'", true));
        j.buildAndAssertSuccess(job);
        job.setDefinition(new CpsFlowDefinition("error 'Expected history failure'", true));
        j.assertBuildStatus(Result.FAILURE, job.scheduleBuild2(0).get());
        job.setDefinition(new CpsFlowDefinition("echo 'Latest successful run'", true));
        j.buildAndAssertSuccess(job);
        board.goTo().hasJobsCount(1);
        var tile = p.locator(".bm-cell");
        tile.click(new Locator.ClickOptions().setButton(MouseButton.RIGHT));
        var menu = p.getByRole(AriaRole.MENU);
        assertThat(menu.getByRole(AriaRole.MENUITEM)).hasCount(12);
        assertThat(menu.getByRole(
                        AriaRole.MENUITEM,
                        new Locator.GetByRoleOptions().setName("Run").setExact(true)))
                .isEnabled();
        assertThat(menu.locator(".bm-build-history__details").first()).hasAttribute("aria-label", "#6 Failed");
        assertThat(menu.locator(".bm-build-history__details").last()).hasAttribute("aria-label", "#2 Passed");
        assertThat(menu.locator("time")).hasCount(5);
        assertEquals(
                job.getBuildByNumber(6).getStartTimeInMillis(),
                Instant.parse(menu.locator("time").first().getAttribute("datetime"))
                        .toEpochMilli());
        assertThat(menu.locator("[title='Build duration']").first())
                .hasText(new HumanReadableDuration(job.getBuildByNumber(6).getDuration()).value());
        p.keyboard().press("ArrowUp");
        assertThat(menu.getByRole(
                        AriaRole.MENUITEM,
                        new Locator.GetByRoleOptions().setName("Run").setExact(true)))
                .isFocused();
        p.keyboard().press("ArrowUp");
        assertThat(menu.locator(".bm-build-history__heading a")).isFocused();
        p.keyboard().press("ArrowDown");
        assertThat(menu.getByRole(
                        AriaRole.MENUITEM,
                        new Locator.GetByRoleOptions().setName("Run").setExact(true)))
                .isFocused();
        p.keyboard().press("ArrowDown");
        assertThat(menu.locator(".bm-build-history__details").first()).isFocused();
        p.keyboard().press("ArrowDown");
        assertThat(menu.getByRole(
                        AriaRole.MENUITEM,
                        new Locator.GetByRoleOptions()
                                .setName("Open build #6 page")
                                .setExact(true)))
                .isFocused();
        assertNotEquals(
                menu.locator(".bm-build-history__run")
                        .first()
                        .evaluate("element => getComputedStyle(element).backgroundColor"),
                menu.locator(".bm-build-history__run")
                        .last()
                        .evaluate("element => getComputedStyle(element).backgroundColor"));
        Page html = p.context()
                .waitForPage(() -> menu.getByRole(
                                AriaRole.MENUITEM,
                                new Locator.GetByRoleOptions()
                                        .setName("#4 Passed")
                                        .setExact(true))
                        .click());
        assertThat(html).hasURL(jobUrl + "4/HTML_20Report/details/test%20result.html");
        assertThat(html.locator("h1")).hasText("Custom report entry");
        assertEquals(
                p.context().newCDPSession(p).send("Browser.getWindowForTarget").get("windowId"),
                p.context()
                        .newCDPSession(html)
                        .send("Browser.getWindowForTarget")
                        .get("windowId"),
                "History links must open a tab in the same browser window");
        html.close();
        assertThat(menu).hasCount(0);
        tile.click(new Locator.ClickOptions().setButton(MouseButton.RIGHT));
        Page buildPage = p.context()
                .waitForPage(() -> menu.getByRole(
                                AriaRole.MENUITEM,
                                new Locator.GetByRoleOptions()
                                        .setName("Open build #4 page")
                                        .setExact(true))
                        .click());
        assertThat(buildPage).hasURL(jobUrl + "4/");
        assertThat(p).hasURL(view.get().getAbsoluteUrl());
        buildPage.close();
        tile.click(new Locator.ClickOptions().setButton(MouseButton.RIGHT));
        Page jobPage = p.context()
                .waitForPage(() -> menu.locator(".bm-build-history__heading a").click());
        assertThat(jobPage).hasURL(jobUrl);
        jobPage.close();
        tile.click(new Locator.ClickOptions().setButton(MouseButton.RIGHT));
        Page console = p.context()
                .waitForPage(() -> p.getByRole(
                                AriaRole.MENUITEM,
                                new Page.GetByRoleOptions().setName("#3 Passed").setExact(true))
                        .click());
        assertThat(console).hasURL(jobUrl + "3/console");
        console.close();
        tile.click(new Locator.ClickOptions().setButton(MouseButton.RIGHT));
        p.bringToFront();
        Page backgroundHistory = p.context()
                .waitForPage(() -> p.getByRole(
                                AriaRole.MENUITEM,
                                new Page.GetByRoleOptions().setName("#4 Passed").setExact(true))
                        .click(new Locator.ClickOptions().setModifiers(List.of(KeyboardModifier.CONTROLORMETA))));
        assertThat(backgroundHistory).hasURL(jobUrl + "4/HTML_20Report/details/test%20result.html");
        // Playwright emulates focus for every tab; real selected-tab behavior is checked in Chrome.
        assertThat(p).hasURL(view.get().getAbsoluteUrl());
        assertThat(menu).isVisible();
        backgroundHistory.close();
        Page middleHistory = p.context()
                .waitForPage(() -> p.getByRole(
                                AriaRole.MENUITEM,
                                new Page.GetByRoleOptions().setName("#3 Passed").setExact(true))
                        .click(new Locator.ClickOptions().setButton(MouseButton.MIDDLE)));
        assertThat(middleHistory).hasURL(jobUrl + "3/console");
        assertThat(p).hasURL(view.get().getAbsoluteUrl());
        middleHistory.close();
        p.keyboard().down("Shift");
        try {
            Page shiftedHistory = p.context()
                    .waitForPage(() -> p.getByRole(
                                    AriaRole.MENUITEM,
                                    new Page.GetByRoleOptions()
                                            .setName("#4 Passed")
                                            .setExact(true))
                            .click());
            assertThat(shiftedHistory).hasURL(jobUrl + "4/HTML_20Report/details/test%20result.html");
            assertEquals(
                    p.context()
                            .newCDPSession(p)
                            .send("Browser.getWindowForTarget")
                            .get("windowId"),
                    p.context()
                            .newCDPSession(shiftedHistory)
                            .send("Browser.getWindowForTarget")
                            .get("windowId"),
                    "History Shift-click must open a tab while Shift remains held");
            shiftedHistory.close();
        } finally {
            p.keyboard().up("Shift");
        }
        p.keyboard().down("Shift");
        try {
            Page shifted = p.context().waitForPage(() -> tile.click());
            assertThat(shifted).hasURL(jobUrl + "7/console");
            assertEquals(
                    p.context()
                            .newCDPSession(p)
                            .send("Browser.getWindowForTarget")
                            .get("windowId"),
                    p.context()
                            .newCDPSession(shifted)
                            .send("Browser.getWindowForTarget")
                            .get("windowId"),
                    "Shift-click must open a tab while Shift remains held");
            shifted.close();
        } finally {
            p.keyboard().up("Shift");
        }
        tile.dispatchEvent("pointerdown", Map.of("pointerType", "touch", "clientX", 100, "clientY", 100));
        assertThat(p.getByRole(AriaRole.MENU)).isVisible();
        tile.dispatchEvent("pointerup", Map.of("pointerType", "touch"));
        tile.dispatchEvent("click");
        assertThat(p).hasURL(view.get().getAbsoluteUrl());
        p.keyboard().press("Escape");
        assertThat(p.getByRole(AriaRole.MENU)).hasCount(0);
        p.setViewportSize(320, 640);
        tile.click(new Locator.ClickOptions().setButton(MouseButton.RIGHT));
        var bounds = menu.boundingBox();
        assertTrue(bounds.x >= 0 && bounds.x + bounds.width <= 320);
        assertTrue(bounds.y >= 0 && bounds.y + bounds.height <= 640);
        assertTrue((Boolean) menu.evaluate("element => element.scrollWidth <= element.clientWidth"));
    }
}

package com.groupsolver.jenkins.plugins.buildboard.e2e;

import static com.groupsolver.jenkins.plugins.buildboard.e2e.utils.BuildBoardViewUtils.createBuildBoardView;
import static com.microsoft.playwright.assertions.PlaywrightAssertions.assertThat;

import com.cloudbees.hudson.plugins.folder.Folder;
import com.groupsolver.jenkins.plugins.buildboard.e2e.config.PlaywrightConfig;
import com.groupsolver.jenkins.plugins.buildboard.e2e.pages.BuildBoardViewPage;
import com.microsoft.playwright.Page;
import com.microsoft.playwright.junit.UsePlaywright;
import hudson.FilePath;
import java.nio.file.Path;
import org.jenkinsci.plugins.workflow.cps.CpsFlowDefinition;
import org.jenkinsci.plugins.workflow.job.WorkflowJob;
import org.junit.jupiter.api.Test;
import org.jvnet.hudson.test.JenkinsRule;
import org.jvnet.hudson.test.junit.jupiter.WithJenkins;

@WithJenkins
@UsePlaywright(PlaywrightConfig.class)
class HtmlReportNavigationIT {
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
        p.locator(".bm-cell").click();
        assertThat(p).hasURL(jobUrl);

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
        p.locator(".bm-cell")
                .getByText("#1", new com.microsoft.playwright.Locator.GetByTextOptions().setExact(true))
                .click();
        assertThat(p).hasURL(jobUrl + "1/HTML_20Report/index.html");
        assertThat(p.locator("h1")).hasText("Latest HTML result");

        job.setDefinition(new CpsFlowDefinition("echo 'No report for this run'", true));
        j.buildAndAssertSuccess(job);
        board.goTo().hasJobsCount(1);
        p.locator(".bm-cell").click();
        assertThat(p).hasURL(jobUrl + "2/console");
        assertThat(p.locator(".console-output")).containsText("No report for this run");

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
        p.locator(".bm-cell").click();
        assertThat(p).hasURL(jobUrl + "3/console");

        job.setDefinition(new CpsFlowDefinition("""
                node {
                  writeFile file: 'report/details/test result.html', text: '<h1>Custom report entry</h1>'
                  publishHTML(target: [reportName: 'HTML Report', reportDir: 'report', reportFiles: 'details/*.html',
                    keepAll: true, alwaysLinkToLastBuild: true, allowMissing: false])
                }
                """, true));
        j.buildAndAssertSuccess(job);
        board.goTo().hasJobsCount(1);
        p.locator(".bm-cell").click();
        assertThat(p).hasURL(jobUrl + "4/HTML_20Report/details/test%20result.html");
        assertThat(p.locator("h1")).hasText("Custom report entry");
    }
}

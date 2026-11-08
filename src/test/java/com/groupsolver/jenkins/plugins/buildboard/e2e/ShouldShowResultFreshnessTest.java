package com.groupsolver.jenkins.plugins.buildboard.e2e;

import static com.groupsolver.jenkins.plugins.buildboard.e2e.utils.BuildBoardViewUtils.createBuildBoardView;
import static com.microsoft.playwright.assertions.PlaywrightAssertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;

import com.groupsolver.jenkins.plugins.buildboard.e2e.config.PlaywrightConfig;
import com.groupsolver.jenkins.plugins.buildboard.e2e.pages.BuildBoardViewPage;
import com.microsoft.playwright.Page;
import com.microsoft.playwright.junit.UsePlaywright;
import hudson.triggers.TimerTrigger;
import java.time.LocalDate;
import java.time.ZoneOffset;
import org.jenkinsci.plugins.workflow.cps.CpsFlowDefinition;
import org.jenkinsci.plugins.workflow.job.WorkflowJob;
import org.junit.jupiter.api.Test;
import org.jvnet.hudson.test.JenkinsRule;
import org.jvnet.hudson.test.junit.jupiter.WithJenkins;

@WithJenkins
@UsePlaywright(PlaywrightConfig.class)
class ShouldShowResultFreshnessTest {
    @Test
    void exposesScheduledPipelineFreshnessAndPreservesDisabledAppearance(Page p, JenkinsRule j) throws Exception {
        p.onPageError(error -> {
            throw new AssertionError(error);
        });
        WorkflowJob job = j.createProject(WorkflowJob.class, "TEST_PROD_freshness");
        job.setDefinition(new CpsFlowDefinition("echo 'fresh result'", true));
        LocalDate scheduledDate = LocalDate.now(ZoneOffset.UTC).plusDays(2);
        job.addTrigger(new TimerTrigger(
                "TZ=UTC\n0 0 %s %s *".formatted(scheduledDate.getDayOfMonth(), scheduledDate.getMonthValue())));
        j.buildAndAssertSuccess(job);
        var view = createBuildBoardView(j, "Freshness").addJobs(job);
        view.get().getConfig().setAutoRefreshEvery(1);
        BuildBoardViewPage.from(p, view).goTo().hasJobsCount(1);
        assertThat(p.locator(".bm-cell")).hasCSS("opacity", "1");
        Object opacity = p.evaluate("""
                async () => {
                  const response = await fetch('fetchJobViews');
                  const { data } = await response.json();
                  return data[0].resultOpacity;
                }
                """);
        assertEquals(1, ((Number) opacity).doubleValue());
        job.setDisabled(true);
        assertThat(p.locator(".bm-cell")).hasCSS("opacity", "0.4");
        job.setDisabled(false);
        assertThat(p.locator(".bm-cell")).hasCSS("opacity", "1");
    }
}

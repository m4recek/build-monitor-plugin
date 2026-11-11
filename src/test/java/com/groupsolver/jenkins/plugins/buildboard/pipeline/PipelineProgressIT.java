package com.groupsolver.jenkins.plugins.buildboard.pipeline;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.groupsolver.jenkins.plugins.buildboard.e2e.config.PlaywrightConfig;
import com.groupsolver.jenkins.plugins.buildboard.e2e.pages.BuildBoardViewPage;
import com.groupsolver.jenkins.plugins.buildboard.e2e.utils.BuildBoardViewUtils;
import com.microsoft.playwright.Page;
import com.microsoft.playwright.junit.UsePlaywright;
import java.util.List;
import java.util.stream.StreamSupport;
import org.jenkinsci.plugins.workflow.actions.TimingAction;
import org.jenkinsci.plugins.workflow.cps.CpsFlowDefinition;
import org.jenkinsci.plugins.workflow.graph.FlowGraphWalker;
import org.jenkinsci.plugins.workflow.graph.FlowNode;
import org.jenkinsci.plugins.workflow.job.WorkflowJob;
import org.jenkinsci.plugins.workflow.job.WorkflowRun;
import org.jenkinsci.plugins.workflow.support.actions.PauseAction;
import org.junit.jupiter.api.Test;
import org.jvnet.hudson.test.JenkinsRule;
import org.jvnet.hudson.test.junit.jupiter.WithJenkins;

@WithJenkins
@UsePlaywright(PlaywrightConfig.class)
class PipelineProgressIT {
    @Test
    void readsRealStageBudgetsAndStopsAccumulatingDuringRecordedPauses(JenkinsRule j, Page p) throws Exception {
        WorkflowJob job = j.createProject(WorkflowJob.class, "Stage timings");
        job.setDefinition(new CpsFlowDefinition("""
                stage('Build') { sleep 1 }
                stage('Test') { sleep 1 }
                stage('Publish') { sleep 1 }
                stage('Finish') { echo 'finished' }
                """, true));
        j.buildAndAssertSuccess(job);
        job.setDefinition(new CpsFlowDefinition("""
                stage('Build') { echo 'built' }
                stage('Test') { echo 'holding test stage'; sleep 1000 }
                stage('Publish') { echo 'publish' }
                stage('Finish') { echo 'finished' }
                """, true));
        WorkflowRun run = job.scheduleBuild2(0).waitForStart();
        try {
            j.waitForMessage("Sleeping for", run);
            List<FlowNode> nodes = StreamSupport.stream(new FlowGraphWalker(run.getExecution()).spliterator(), false)
                    .toList();
            FlowNode sleep = nodes.stream()
                    .filter(node -> node.getDisplayFunctionName().equals("sleep"))
                    .findFirst()
                    .orElseThrow();
            PauseAction pause = new PauseAction("Executor wait");
            pause.setStartTime(TimingAction.getStartTime(sleep));
            sleep.addAction(pause);
            var before = PipelineProgress.of(run, pause.getStartTime() + 10);
            var after = PipelineProgress.of(run, pause.getStartTime() + 40 * 60 * 1000);
            assertFalse(before.indeterminate());
            assertEquals(3, before.boundaries().size());
            assertEquals(before.percentage(), after.percentage());
            assertEquals(before.boundaries(), after.boundaries());
            assertTrue(after.percentage() <= after.boundaries().get(1));
            assertTrue(after.percentage() >= after.boundaries().get(0));
            var view = BuildBoardViewUtils.createBuildBoardView(j, "Stage progress")
                    .addJobs(job);
            BuildBoardViewPage.from(p, view).goTo().hasJobsCount(1);
            var ticks = p.locator(".bm-cell-stage-boundary");
            com.microsoft.playwright.assertions.PlaywrightAssertions.assertThat(ticks)
                    .hasCount(3);
            com.microsoft.playwright.assertions.PlaywrightAssertions.assertThat(ticks.first())
                    .hasCSS("width", "2px");
            com.microsoft.playwright.assertions.PlaywrightAssertions.assertThat(ticks.first())
                    .hasCSS("height", "7px");
            com.microsoft.playwright.assertions.PlaywrightAssertions.assertThat(ticks.first())
                    .hasCSS("opacity", "0.5");
            assertEquals(true, p.evaluate("""
                    () => {
                      const cell = document.querySelector('.bm-cell');
                      const text = [...cell.querySelectorAll('h2, .bm-cell__details')];
                      const geometry = () => text.map(element => {
                        const rect = element.getBoundingClientRect();
                        return [rect.x, rect.y, rect.width, rect.height];
                      });
                      const before = geometry();
                      cell.querySelectorAll('.bm-cell-stage-boundary').forEach(tick => tick.remove());
                      return JSON.stringify(before) === JSON.stringify(geometry());
                    }
                    """));
        } finally {
            run.doStop();
            j.waitForCompletion(run);
        }
    }
}

package com.groupsolver.jenkins.plugins.buildboard.e2e;

import static com.groupsolver.jenkins.plugins.buildboard.e2e.utils.BuildBoardViewUtils.createBuildBoardView;
import static com.microsoft.playwright.assertions.PlaywrightAssertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.cloudbees.hudson.plugins.folder.Folder;
import com.groupsolver.jenkins.plugins.buildboard.e2e.config.PlaywrightConfig;
import com.groupsolver.jenkins.plugins.buildboard.e2e.pages.BuildBoardViewPage;
import com.microsoft.playwright.Locator;
import com.microsoft.playwright.Page;
import com.microsoft.playwright.junit.UsePlaywright;
import com.microsoft.playwright.options.AriaRole;
import com.microsoft.playwright.options.MouseButton;
import hudson.model.Result;
import org.jenkinsci.plugins.workflow.cps.CpsFlowDefinition;
import org.jenkinsci.plugins.workflow.job.WorkflowJob;
import org.jenkinsci.plugins.workflow.job.WorkflowRun;
import org.junit.jupiter.api.Test;
import org.jvnet.hudson.test.JenkinsRule;
import org.jvnet.hudson.test.junit.jupiter.WithJenkins;

@WithJenkins
@UsePlaywright(PlaywrightConfig.class)
class ShouldCancelRunningBuildTest {
    @Test
    void cancelsOnlyTheSelectedRunningBuildAndUpdatesTheOpenMenu(Page p, JenkinsRule j) throws Exception {
        Folder folder = j.createProject(Folder.class, "Tests");
        WorkflowJob job = folder.createProject(WorkflowJob.class, "TEST_PROD_cancel");
        job.setDefinition(new CpsFlowDefinition("echo 'Ready for cancellation'; sleep 1000", true));
        var view = createBuildBoardView(j, "Cancellation").displayAllProjects().displayNestedProjectsFromSubfolders();
        view.get().getConfig().setAutoRefreshEvery(1);
        WorkflowRun first = job.scheduleBuild2(0).waitForStart();
        j.waitForMessage("Sleeping for", first);
        WorkflowRun second = job.scheduleBuild2(0).waitForStart();
        try {
            j.waitForMessage("Sleeping for", second);
            BuildBoardViewPage.from(p, view).goTo().hasJobsCount(1);
            p.locator(".bm-cell").click(new Locator.ClickOptions().setButton(MouseButton.RIGHT));
            var cancelSecond = p.getByRole(
                    AriaRole.MENUITEM,
                    new Page.GetByRoleOptions().setName("Cancel build #2").setExact(true));
            assertThat(cancelSecond).isEnabled();
            String alignedActions = """
                    () => {
                      const cancel = document.querySelector('.bm-build-history__cancel-button');
                      const run = document.querySelector('.bm-build-history__run-button');
                      const a = cancel.getBoundingClientRect();
                      const b = run.getBoundingClientRect();
                      return a.right <= b.left && Math.abs(a.top - b.top) < 0.01
                        && Math.abs(a.height - b.height) < 0.01
                        && (cancel.compareDocumentPosition(run) & Node.DOCUMENT_POSITION_FOLLOWING) !== 0;
                    }
                    """;
            assertTrue((Boolean) p.evaluate(alignedActions), "Cancel must precede Run with matching button heights");
            assertTrue((Boolean) cancelSecond.evaluate("""
                    button => {
                      const probe = document.createElement('span');
                      probe.style.color = 'var(--destructive-color)';
                      button.append(probe);
                      const matches = getComputedStyle(button).color === getComputedStyle(probe).color;
                      probe.remove();
                      return matches;
                    }
                    """));
            p.setViewportSize(320, 640);
            p.locator(".bm-cell").click(new Locator.ClickOptions().setButton(MouseButton.RIGHT));
            assertTrue((Boolean) p.evaluate(alignedActions), "Buttons must remain aligned at 320px");
            assertTrue((Boolean)
                    p.locator(".bm-build-history").evaluate("element => element.scrollWidth <= element.clientWidth"));
            cancelSecond.click();
            j.waitForCompletion(second);
            j.assertBuildStatus(Result.ABORTED, second);
            assertTrue(first.isBuilding(), "Canceling #2 must leave concurrent #1 running");
            assertThat(p).hasURL(view.get().getAbsoluteUrl());
            var cancelFirst = p.getByRole(
                    AriaRole.MENUITEM,
                    new Page.GetByRoleOptions().setName("Cancel build #1").setExact(true));
            assertThat(cancelFirst).isEnabled();
            cancelFirst.click();
            j.waitForCompletion(first);
            j.assertBuildStatus(Result.ABORTED, first);
            assertThat(p.locator(".bm-build-history__cancel-button")).hasCount(0);
            assertThat(p.getByRole(
                            AriaRole.MENUITEM,
                            new Page.GetByRoleOptions().setName("Run").setExact(true)))
                    .isEnabled();
            assertThat(p).hasURL(view.get().getAbsoluteUrl());
        } finally {
            if (first.isBuilding()) {
                first.doStop();
                j.waitForCompletion(first);
            }
            if (second.isBuilding()) {
                second.doStop();
                j.waitForCompletion(second);
            }
        }
    }
}

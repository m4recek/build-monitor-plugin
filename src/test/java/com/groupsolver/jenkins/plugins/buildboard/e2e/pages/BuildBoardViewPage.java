package com.groupsolver.jenkins.plugins.buildboard.e2e.pages;

import static com.microsoft.playwright.assertions.PlaywrightAssertions.assertThat;

import com.groupsolver.jenkins.plugins.buildboard.e2e.utils.BuildBoardViewUtils;
import com.microsoft.playwright.Locator;
import com.microsoft.playwright.Page;
import com.microsoft.playwright.options.AriaRole;
import java.util.List;

public class BuildBoardViewPage extends JenkinsPage<BuildBoardViewPage> {

    private BuildBoardViewPage(Page page, BuildBoardViewUtils.FluentBuildBoardView view) {
        super(page, view.get().getAbsoluteUrl());
    }

    public static BuildBoardViewPage from(Page page, BuildBoardViewUtils.FluentBuildBoardView view) {
        return new BuildBoardViewPage(page, view);
    }

    @Override
    BuildBoardViewPage waitForLoaded() {
        super.waitForLoaded();
        Locator content = page.locator(".bm-grid-viewport, .jenkins-notice").first();
        assertThat(content).isVisible();
        return this;
    }

    public BuildBoardViewPage hasJobsCount(int jobsCount) {
        Locator cells = page.locator(".bm-cell");
        assertThat(cells).hasCount(jobsCount);
        return this;
    }

    public JobComponent getJob(String jobName) {
        Locator job = page.locator(".bm-cell h2")
                .getByText(jobName)
                .locator("xpath=ancestor::*[contains(@class, 'bm-cell')]");
        return JobComponent.from(job);
    }

    public BuildBoardViewPage hasJob(String jobName) {
        Locator link = page.locator(".bm-cell h2").getByText(jobName);
        assertThat(link).isVisible();
        return this;
    }

    public BuildBoardViewPage hasNoJobs() {
        Locator heading = page.getByText("It seems a bit empty here...");
        Locator link = page.getByRole(AriaRole.LINK, new Page.GetByRoleOptions().setName("Add some jobs"));
        assertThat(heading).isVisible();
        assertThat(link).isVisible();
        return this;
    }

    public BuildBoardViewPage hasPageCount(int pageCount) {
        Locator pages = page.locator(".bm-grid-page");
        assertThat(pages).hasCount(pageCount);
        return this;
    }

    public BuildBoardViewPage scrollPastPageBoundaryTowards(int pageNumber) {
        int pageIndex = pageNumber - 1;
        Locator viewport = page.locator(".bm-grid-viewport");
        viewport.evaluate(
                "(element, pageIndex) => element.scrollTo({ left: element.clientWidth * pageIndex + 120 })", pageIndex);
        return this;
    }

    public BuildBoardViewPage hasSnappedToPage(int pageNumber) {
        int pageIndex = pageNumber - 1;

        page.waitForFunction(
                "([selector, pageIndex]) => {"
                        + "  const element = document.querySelector(selector);"
                        + "  return !!element && Math.abs(element.scrollLeft - (element.clientWidth * pageIndex)) < 4;"
                        + "}",
                List.of(".bm-grid-viewport", pageIndex));

        return this;
    }

    public BuildBoardViewPage hasActivePage(int pageNumber) {
        Locator dot = page.locator(".bm-grid-pagination__dot").nth(pageNumber - 1);
        assertThat(dot).hasAttribute("aria-current", "page");
        return this;
    }
}

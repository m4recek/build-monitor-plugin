package com.groupsolver.jenkins.plugins.buildboard.e2e;

import static com.groupsolver.jenkins.plugins.buildboard.e2e.utils.BuildBoardViewUtils.createBuildBoardView;
import static com.groupsolver.jenkins.plugins.buildboard.e2e.utils.FolderUtils.createFolder;

import com.groupsolver.jenkins.plugins.buildboard.e2e.config.PlaywrightConfig;
import com.groupsolver.jenkins.plugins.buildboard.e2e.pages.BuildBoardViewPage;
import com.microsoft.playwright.Page;
import com.microsoft.playwright.junit.UsePlaywright;
import org.junit.jupiter.api.Test;
import org.jvnet.hudson.test.JenkinsRule;
import org.jvnet.hudson.test.junit.jupiter.WithJenkins;

@WithJenkins
@UsePlaywright(PlaywrightConfig.class)
class ShouldSupportCloudBeesFoldersTest {

    @Test
    void test(Page p, JenkinsRule j) {
        createFolder(j, "Search Services")
                .createFreeStyleProject("Librarian")
                .createFolder("Contracts")
                .createFreeStyleProject("Third Party System");
        var view = createBuildBoardView(j, "Build Monitor").displayAllProjects().displayNestedProjectsFromSubfolders();

        BuildBoardViewPage.from(p, view)
                .goTo()
                .hasJob("Search Services » Librarian")
                .hasJob("Search Services » Contracts » Third Party System");
    }
}

package com.groupsolver.jenkins.plugins.buildboard.e2e.utils;

import com.groupsolver.jenkins.plugins.buildboard.BuildBoardView;
import hudson.model.TopLevelItem;
import java.io.IOException;
import org.jvnet.hudson.test.JenkinsRule;

public class BuildBoardViewUtils {

    public static FluentBuildBoardView createBuildBoardView(JenkinsRule jenkins, String name) {
        try {
            BuildBoardView view = new BuildBoardView(name, null);
            jenkins.getInstance().addView(view);
            return new FluentBuildBoardView(
                    (BuildBoardView) jenkins.getInstance().getView(name));
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public static class FluentBuildBoardView {
        private final BuildBoardView view;

        public FluentBuildBoardView(BuildBoardView view) {
            this.view = view;
        }

        public FluentBuildBoardView addJobs(TopLevelItem... project) {
            try {
                for (TopLevelItem topLevelItem : project) {
                    view.add(topLevelItem);
                }
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
            return this;
        }

        public FluentBuildBoardView displayAllProjects() {
            view.setIncludeRegex(".*");
            return this;
        }

        public FluentBuildBoardView displayNestedProjectsFromSubfolders() {
            view.setRecurse(true);
            return this;
        }

        public FluentBuildBoardView withMaximumColumns(int maxColumns) {
            view.getConfig().setMaxColumns(maxColumns);
            return this;
        }

        public FluentBuildBoardView withTextScale(double textScale) {
            view.getConfig().setTextScale(textScale);
            return this;
        }

        public BuildBoardView get() {
            return view;
        }
    }
}

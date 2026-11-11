package com.groupsolver.jenkins.plugins.buildboard.viewmodel;

import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.groupsolver.jenkins.plugins.buildboard.facade.RelativeLocation;
import com.groupsolver.jenkins.plugins.buildboard.pipeline.PipelineProgress;
import com.groupsolver.jenkins.plugins.buildboard.viewmodel.duration.Duration;
import com.groupsolver.jenkins.plugins.buildboard.viewmodel.duration.HumanReadableDuration;
import com.groupsolver.jenkins.plugins.buildboard.viewmodel.features.Feature;
import hudson.FilePath;
import hudson.Util;
import hudson.model.Action;
import hudson.model.Item;
import hudson.model.Job;
import hudson.model.Result;
import hudson.model.Run;
import hudson.util.RunList;
import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.Date;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;
import jenkins.model.ParameterizedJobMixIn;
import org.jenkinsci.plugins.workflow.job.WorkflowRun;

/**
 * @author Jan Molak
 */
@JsonSerialize(using = JobViewSerialiser.class)
public class JobView {
    private final Date systemTime;
    private final Job<?, ?> job;
    private final boolean isPipelineJob;
    private final RelativeLocation relative;
    private final PipelineProgress stageProgress;

    private final List<Feature> features = new ArrayList<>();

    public static JobView of(Job<?, ?> job, List<Feature> features, boolean isPipelineJob) {
        return new JobView(job, features, isPipelineJob, RelativeLocation.of(job), new Date());
    }

    public JobView(
            Job<?, ?> job, List<Feature> features, boolean isPipelineJob, RelativeLocation relative, Date systemTime) {
        this.job = job;
        this.isPipelineJob = isPipelineJob;
        this.relative = relative;
        this.systemTime = systemTime;
        this.stageProgress = isPipelineJob && job.getLastBuild() instanceof WorkflowRun run && run.isBuilding()
                ? PipelineProgress.of(run, systemTime.getTime())
                : null;

        for (Feature feature : features) {
            this.features.add(feature.of(this));
        }
    }

    public List<Feature> features() {
        return List.copyOf(features);
    }

    public <F extends Feature> F which(Class<F> requestedFeature) {
        for (Feature feature : features) {
            if (feature.getClass() == requestedFeature) {
                return (F) feature;
            }
        }

        throw new RuntimeException(String.format(
                "%s is not a feature of this project: '%s'", requestedFeature.getSimpleName(), job.getName()));
    }

    public String name() {
        return relative.name();
    }

    public String url() {
        return relative.url();
    }

    public String targetUrl() throws IOException {
        return targetUrl(job.getLastBuild());
    }

    public String runUrl() {
        if (job instanceof ParameterizedJobMixIn.ParameterizedJob<?, ?> buildable) {
            if (buildable.isBuildable() && job.hasPermission(Item.BUILD)) {
                String action = buildable.isParameterized() ? "buildWithParameters" : "build";
                return url().replaceAll("/+$", "") + "/" + action + "?delay=0sec";
            }
        }
        return null;
    }

    public List<PreviousBuild> previousBuilds() throws IOException {
        List<PreviousBuild> builds = new ArrayList<>();
        Run<?, ?> latest = job.getLastBuild();
        Run<?, ?> build = latest == null ? null : latest.getPreviousBuild();
        while (build != null && builds.size() < 5) {
            builds.add(new PreviousBuild(
                    build.getDisplayName(),
                    buildUrl(build),
                    targetUrl(build),
                    build.isBuilding() ? "running" : CssStatus.of(build.getResult()),
                    build.getStartTimeInMillis(),
                    new HumanReadableDuration(
                                    build.isBuilding()
                                            ? Math.max(0, systemTime.getTime() - build.getStartTimeInMillis())
                                            : build.getDuration())
                            .value()));
            build = build.getPreviousBuild();
        }
        return builds;
    }

    public record PreviousBuild(
            String name, String url, String targetUrl, String status, long startedAt, String duration) {}

    private String buildUrl(Run<?, ?> build) {
        return url().replaceAll("/+$", "") + "/" + build.getNumber() + "/";
    }

    private String targetUrl(Run<?, ?> build) throws IOException {
        if (build == null) {
            return url();
        }

        String buildUrl = buildUrl(build);
        for (Action action : build.getAllActions()) {
            if (action.getClass().getName().equals("htmlpublisher.HtmlPublisherTarget$HTMLBuildAction")
                    && action.getDisplayName() != null) {
                try {
                    Object target = action.getClass().getMethod("getHTMLTarget").invoke(action);
                    FilePath archive = (FilePath) target.getClass()
                            .getMethod("getArchiveTarget", Run.class)
                            .invoke(target, build);
                    String reportFiles = (String)
                            target.getClass().getMethod("getReportFiles").invoke(target);
                    Path directory = Path.of(archive.getRemote()).normalize();
                    for (String pattern : reportFiles.split(",")) {
                        FilePath[] reports = archive.list(pattern.trim());
                        Arrays.sort(reports, Comparator.comparing(FilePath::getRemote));
                        for (FilePath report : reports) {
                            Path path = Path.of(report.getRemote()).normalize();
                            if (path.startsWith(directory) && report.isDirectory() == false) {
                                String relativePath =
                                        directory.relativize(path).toString().replace('\\', '/');
                                String encodedPath = Arrays.stream(relativePath.split("/"))
                                        .map(Util::rawEncode)
                                        .collect(Collectors.joining("/"));
                                return buildUrl + action.getUrlName() + "/" + encodedPath;
                            }
                        }
                    }
                } catch (ReflectiveOperationException error) {
                    throw new IOException("Cannot read the published HTML report configuration", error);
                } catch (InterruptedException error) {
                    Thread.currentThread().interrupt();
                    throw new IOException("Interrupted while locating the published HTML report", error);
                }
            }
        }
        return buildUrl + "console";
    }

    public String status() {
        return CssStatus.of(this);
    }

    public String estimatedDuration() {
        return formatted(lastBuild().estimatedDuration());
    }

    public String timeElapsedSinceLastBuild() {
        return formatted(lastCompletedBuild().timeElapsedSince());
    }

    private String formatted(Duration duration) {
        return null != duration ? duration.value() : "";
    }

    public double resultOpacity() {
        return ResultFreshness.opacity(job, systemTime.getTime());
    }

    public int progress() {
        if (stageProgress != null) {
            return (int) stageProgress.percentage();
        }
        return lastBuild().progress();
    }

    public PipelineProgress stageProgress() {
        return stageProgress;
    }

    public boolean isDisabled() {
        return !(isJobBuildable() || isJobExternal());
    }

    private boolean isJobBuildable() {
        return job.isBuildable();
    }

    private boolean isJobExternal() {
        return job instanceof hudson.model.ViewJob;
    }

    public boolean isRunning() {
        return lastBuild().isRunning();
    }

    public Result lastResult() {
        return lastCompletedBuild().result();
    }

    // todo track by job.hashCode messes up the animation
    @Override
    public int hashCode() {
        return Objects.hashCode(job.hashCode());
    }

    @Override
    public boolean equals(Object obj) {
        return Objects.equals(job, obj);
    }

    @Override
    public String toString() {
        return name();
    }

    // --

    @SuppressWarnings("unchecked")
    public List<BuildViewModel> currentBuilds() {
        List<BuildViewModel> currentBuilds = new ArrayList<>();

        RunList<Run<?, ?>> runList = ((RunList<Run<?, ?>>) job.getNewBuilds()).filter(BuildingPredicate.INSTANCE);

        for (Run<?, ?> run : runList) {
            currentBuilds.add(buildViewOf(run));
        }

        return currentBuilds;
    }

    public BuildViewModel lastBuild() {
        return buildViewOf(job.getLastBuild());
    }

    public BuildViewModel lastCompletedBuild() {
        BuildViewModel previousBuild = lastBuild();
        while (previousBuild.isRunning() && previousBuild.hasPreviousBuild()) {
            previousBuild = previousBuild.previousBuild();
        }

        return previousBuild;
    }

    private BuildViewModel buildViewOf(Run<?, ?> build) {
        if (null == build) {
            return new NullBuildView();
        }

        return BuildView.of(build, isPipelineJob, relative, systemTime);
    }
}

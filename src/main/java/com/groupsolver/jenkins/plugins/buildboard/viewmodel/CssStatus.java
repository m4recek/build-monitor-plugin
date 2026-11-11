package com.groupsolver.jenkins.plugins.buildboard.viewmodel;

import hudson.model.Result;
import java.util.Map;

/**
 * @author Jan Molak
 */
public class CssStatus {

    private static final Map<Result, String> statuses = Map.of(
            Result.SUCCESS, "successful",
            Result.UNSTABLE, "unstable",
            Result.FAILURE, "failing",
            Result.NOT_BUILT, "unknown",
            Result.ABORTED, "aborted");

    public static String of(Result result) {
        return result == null ? "unknown" : statuses.getOrDefault(result, "unknown");
    }

    public static String of(final JobView job) {
        String status = of(job.lastResult());

        if (job.isDisabled()) {
            status = "disabled";
        }

        if (job.isRunning()) {
            status = "running";
        }

        return status;
    }
}

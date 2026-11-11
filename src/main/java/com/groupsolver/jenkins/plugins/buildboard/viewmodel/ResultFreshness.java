package com.groupsolver.jenkins.plugins.buildboard.viewmodel;

import hudson.model.Job;
import hudson.model.Run;
import hudson.scheduler.CronTab;
import hudson.scheduler.CronTabList;
import hudson.scheduler.Hash;
import hudson.scheduler.RareOrImpossibleDateException;
import hudson.triggers.TimerTrigger;
import java.util.Calendar;
import java.util.TimeZone;
import jenkins.triggers.TriggeredItem;

/** Presentation freshness between a completed result and its next timer occurrence. */
final class ResultFreshness {
    private ResultFreshness() {}

    static double opacity(Job<?, ?> job, long now) {
        Run<?, ?> completed = job.getLastCompletedBuild();
        if (completed == null || job.isBuilding() || !job.isBuildable() || !(job instanceof TriggeredItem item)) {
            return 1.0;
        }
        long completedAt = completed.getTimeInMillis() + completed.getDuration();
        Long nextScheduledAt = nextScheduledAt(item, completedAt);
        if (nextScheduledAt == null) {
            return 1.0;
        }
        long interval = nextScheduledAt - completedAt;
        long elapsed = Math.max(0, now - completedAt);
        int step = (int) Math.min(8, (double) elapsed * 8 / interval);
        return (10 - step) / 10.0;
    }

    private static Long nextScheduledAt(TriggeredItem job, long completedAt) {
        Long next = null;
        for (var trigger : job.getTriggers().values()) {
            if (!(trigger instanceof TimerTrigger timer)) {
                continue;
            }
            // Match Jenkins' multiline cron parsing and shared, job-specific H seed.
            Hash hash = Hash.from(job.getFullName());
            String timezone = null;
            int lineNumber = 0;
            try {
                for (String entry : timer.getSpec().split("\\r?\\n")) {
                    lineNumber++;
                    String line = entry.trim();
                    if (lineNumber == 1 && line.startsWith("TZ=")) {
                        timezone = CronTabList.getValidTimezone(line.substring(3));
                        if (timezone == null) {
                            return null;
                        }
                        continue;
                    }
                    if (line.isEmpty() || line.startsWith("#")) {
                        continue;
                    }
                    CronTab tab = new CronTab(line, lineNumber, hash, timezone);
                    TimeZone zone = tab.getTimeZone();
                    Calendar calendar = Calendar.getInstance(zone == null ? TimeZone.getDefault() : zone);
                    calendar.setTimeInMillis(completedAt + 1);
                    long candidate = tab.ceil(calendar).getTimeInMillis();
                    if (next == null || candidate < next) {
                        next = candidate;
                    }
                }
            } catch (IllegalArgumentException | RareOrImpossibleDateException exception) {
                // No predictable deadline: do not imply that this result is stale.
                return null;
            }
        }
        return next;
    }
}

package com.groupsolver.jenkins.plugins.buildboard.viewmodel;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.groupsolver.jenkins.plugins.buildboard.facade.RelativeLocation;
import hudson.model.FreeStyleBuild;
import hudson.model.FreeStyleProject;
import hudson.scheduler.CronTabList;
import hudson.scheduler.Hash;
import hudson.triggers.SCMTrigger;
import hudson.triggers.TimerTrigger;
import hudson.triggers.TriggerDescriptor;
import java.time.Instant;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.TimeZone;
import org.junit.jupiter.api.Test;

class ResultFreshnessTest {
    private static long at(String time) {
        return Instant.parse(time).toEpochMilli();
    }

    private FreeStyleProject scheduled(String spec, String completedAt) {
        FreeStyleProject job = mock(FreeStyleProject.class);
        FreeStyleBuild completed = mock(FreeStyleBuild.class);
        when(job.getFullName()).thenReturn("Tests/TEST_PROD_graphql_logic");
        when(job.isBuildable()).thenReturn(true);
        when(job.getLastCompletedBuild()).thenReturn(completed);
        when(completed.getTimeInMillis()).thenReturn(at(completedAt));
        when(job.getTriggers()).thenReturn(Map.of(mock(TriggerDescriptor.class), new TimerTrigger(spec)));
        return job;
    }

    @Test
    void fadesAtEachEighthAndStaysStaleAfterMissedRuns() {
        var job = scheduled("TZ=UTC\n0 * * * *", "2026-11-08T08:00:00Z");
        long completedAt = at("2026-11-08T08:00:00Z");
        long eighth = 60 * 60 * 1000 / 8;
        assertEquals(1.0, ResultFreshness.opacity(job, completedAt - 1));
        for (int step = 0; step <= 8; step++) {
            assertEquals((10 - step) / 10.0, ResultFreshness.opacity(job, completedAt + step * eighth));
            if (step > 0) {
                assertEquals((11 - step) / 10.0, ResultFreshness.opacity(job, completedAt + step * eighth - 1));
            }
        }
        assertEquals(0.2, ResultFreshness.opacity(job, at("2026-11-10T08:00:00Z")));
    }

    @Test
    void startsFromCompletionAndResetsForANewResult() {
        var job = scheduled("TZ=UTC\n0 * * * *", "2026-11-08T08:00:00Z");
        when(job.getLastCompletedBuild().getDuration()).thenReturn(30 * 60 * 1000L);
        assertEquals(1.0, ResultFreshness.opacity(job, at("2026-11-08T08:30:00Z")));
        assertEquals(0.6, ResultFreshness.opacity(job, at("2026-11-08T08:45:00Z")));
        assertEquals(0.2, ResultFreshness.opacity(job, at("2026-11-08T09:00:00Z")));
        when(job.getLastCompletedBuild().getTimeInMillis()).thenReturn(at("2026-11-08T09:00:00Z"));
        when(job.getLastCompletedBuild().getDuration()).thenReturn(0L);
        assertEquals(1.0, ResultFreshness.opacity(job, at("2026-11-08T09:00:00Z")));
    }

    @Test
    void usesTheEarliestLineAndTheLongOvernightInterval() {
        var job = scheduled("TZ=UTC\n# office hours\n0 8,12 * * *\n\n0 16 * * *", "2026-11-08T16:00:00Z");
        assertEquals(0.6, ResultFreshness.opacity(job, at("2026-11-09T00:00:00Z")));
        assertEquals(0.2, ResultFreshness.opacity(job, at("2026-11-09T08:00:00Z")));
        when(job.getLastCompletedBuild().getTimeInMillis()).thenReturn(at("2026-11-09T08:00:00Z"));
        assertEquals(0.6, ResultFreshness.opacity(job, at("2026-11-09T10:00:00Z")));
    }

    @Test
    void usesJobSpecificHashingWithTheSameSeedAcrossLines() {
        String spec = "TZ=UTC\nH 9 * * *\nH 10 * * *";
        var job = scheduled(spec, "2026-11-08T08:00:00Z");
        var tabs = CronTabList.create(spec, Hash.from(job.getFullName()));
        Calendar next = Calendar.getInstance(TimeZone.getTimeZone("UTC"));
        next.setTimeInMillis(at("2026-11-08T08:01:00Z"));
        while (!tabs.check(next)) {
            next.add(Calendar.MINUTE, 1);
        }
        long interval = next.getTimeInMillis() - at("2026-11-08T08:00:00Z");
        assertEquals(0.6, ResultFreshness.opacity(job, at("2026-11-08T08:00:00Z") + interval / 2));
        assertEquals(0.2, ResultFreshness.opacity(job, next.getTimeInMillis()));
    }

    @Test
    void honorsTimezoneAndDaylightSavingChanges() {
        var job = scheduled("TZ=Europe/Bratislava\n0 8 * * *", "2026-10-24T06:00:00Z");
        // Clocks move back overnight: the interval is 25 hours, not 24.
        assertEquals(0.6, ResultFreshness.opacity(job, at("2026-10-24T18:30:00Z")));
        assertEquals(0.3, ResultFreshness.opacity(job, at("2026-10-25T06:00:00Z")));
        assertEquals(0.2, ResultFreshness.opacity(job, at("2026-10-25T07:00:00Z")));
    }

    @Test
    void keepsRunningDisabledAndNeverBuiltJobsAtTheirNormalAppearance() {
        var job = scheduled("TZ=UTC\n0 * * * *", "2026-11-08T08:00:00Z");
        long now = at("2026-11-09T08:00:00Z");
        when(job.isBuilding()).thenReturn(true);
        assertEquals(1.0, ResultFreshness.opacity(job, now));
        when(job.isBuilding()).thenReturn(false);
        when(job.isBuildable()).thenReturn(false);
        assertEquals(1.0, ResultFreshness.opacity(job, now));
        when(job.isBuildable()).thenReturn(true);
        when(job.getLastCompletedBuild()).thenReturn(null);
        assertEquals(1.0, ResultFreshness.opacity(job, now));
    }

    @Test
    void serializesFreshnessAsANumberUsingTheControllerObservationTime() throws Exception {
        var job = scheduled("TZ=UTC\n0 * * * *", "2026-11-08T08:00:00Z");
        var view =
                new JobView(job, List.of(), false, mock(RelativeLocation.class), new Date(at("2026-11-08T08:30:00Z")));
        var json = new ObjectMapper().readTree(new ObjectMapper().writeValueAsString(view));
        assertEquals(0.6, json.get("resultOpacity").doubleValue());
        assertEquals(true, json.get("resultOpacity").isNumber());
    }

    @Test
    void doesNotInventDeadlinesForMissingPollingOrImpossibleSchedules() {
        var job = scheduled("TZ=UTC\n0 0 31 2 *", "2026-11-08T08:00:00Z");
        long now = at("2026-11-09T08:00:00Z");
        assertEquals(1.0, ResultFreshness.opacity(job, now));
        when(job.getTriggers()).thenReturn(Map.of());
        assertEquals(1.0, ResultFreshness.opacity(job, now));
        when(job.getTriggers()).thenReturn(Map.of(mock(TriggerDescriptor.class), new SCMTrigger("* * * * *")));
        assertEquals(1.0, ResultFreshness.opacity(job, now));
    }
}

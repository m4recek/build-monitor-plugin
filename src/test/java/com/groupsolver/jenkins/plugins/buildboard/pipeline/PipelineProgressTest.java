package com.groupsolver.jenkins.plugins.buildboard.pipeline;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;

class PipelineProgressTest {
    @Test
    void capsAnOverdueStageAtItsOwnBudget() {
        var history = List.of(List.of(stage("Build", 40, true), stage("Test", 40, true)));
        var halfway = PipelineProgress.calculate(List.of(stage("Build", 20, false)), history);
        assertEquals(25, halfway.percentage());
        assertEquals(List.of(50.0), halfway.boundaries());
        assertEquals(
                50,
                PipelineProgress.calculate(List.of(stage("Build", 80, false)), history)
                        .percentage());
        assertEquals(
                75,
                PipelineProgress.calculate(List.of(stage("Build", 80, true), stage("Test", 20, false)), history)
                        .percentage());
    }

    @Test
    void budgetsUnequalStagesFromHistoricalExecutionDurations() {
        var history = List.of(
                List.of(stage("A", 10, true), stage("B", 30, true), stage("C", 20, true)),
                List.of(stage("A", 30, true), stage("B", 50, true), stage("C", 20, true)));
        var progress = PipelineProgress.calculate(List.of(stage("A", 900, true), stage("B", 20, false)), history);
        assertEquals(50, progress.percentage());
        assertEquals(List.of(25.0, 75.0), progress.boundaries());
    }

    @Test
    void keepsWaitingAndUnknownEstimatesIndeterminate() {
        var history = List.of(List.of(stage("A", 40, true), stage("B", 40, true)));
        var waiting = PipelineProgress.calculate(List.of(), history);
        assertTrue(waiting.indeterminate());
        assertEquals(0, waiting.percentage());
        assertEquals(List.of(50.0), waiting.boundaries());
        assertTrue(PipelineProgress.calculate(List.of(stage("A", 90000, false)), List.of())
                .indeterminate());
    }

    @Test
    void conservesWidthsAndCapsProgressForOneThroughAThousandStages() {
        for (int count = 1; count <= 1000; count++) {
            List<PipelineProgress.Stage> history = new ArrayList<>();
            double total = 0;
            for (int index = 0; index < count; index++) {
                long duration = (index * 37 + count * 13) % 127 + 1;
                history.add(stage("Stage " + index, duration, true));
                total += duration;
            }
            var waiting = PipelineProgress.calculate(List.of(), List.of(history));
            assertEquals(count - 1, waiting.boundaries().size());
            double previous = 0;
            for (double boundary : waiting.boundaries()) {
                assertTrue(boundary > previous && boundary < 100);
                previous = boundary;
            }
            int current = count / 2;
            List<PipelineProgress.Stage> running = new ArrayList<>(history.subList(0, current));
            running.add(stage(history.get(current).key(), Long.MAX_VALUE, false));
            var overdue = PipelineProgress.calculate(running, List.of(history));
            double budget = history.subList(0, current + 1).stream()
                    .mapToLong(PipelineProgress.Stage::duration)
                    .sum();
            assertEquals(budget / total * 100, overdue.percentage(), 0.0000001);
            assertFalse(overdue.indeterminate());
            assertEquals(
                    100, PipelineProgress.calculate(history, List.of(history)).percentage(), 0.0000001);
        }
    }

    @Test
    void excludesPausedTimeWithoutCountingOverlappingPausesTwice() {
        assertEquals(
                30,
                PipelineProgress.activeDuration(
                        100, 200, List.of(new long[] {110, 140}, new long[] {120, 160}, new long[] {180, 200})));
        assertEquals(0, PipelineProgress.activeDuration(100, 200, List.of(new long[] {0, 1000})));
        assertEquals(100, PipelineProgress.activeDuration(100, 200, List.of()));
    }

    private PipelineProgress.Stage stage(String key, long duration, boolean completed) {
        return new PipelineProgress.Stage(key, duration, completed);
    }
}

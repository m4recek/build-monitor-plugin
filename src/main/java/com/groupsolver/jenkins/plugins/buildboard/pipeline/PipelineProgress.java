package com.groupsolver.jenkins.plugins.buildboard.pipeline;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.jenkinsci.plugins.workflow.actions.LabelAction;
import org.jenkinsci.plugins.workflow.actions.TimingAction;
import org.jenkinsci.plugins.workflow.cps.nodes.StepStartNode;
import org.jenkinsci.plugins.workflow.flow.FlowExecution;
import org.jenkinsci.plugins.workflow.graph.BlockEndNode;
import org.jenkinsci.plugins.workflow.graph.FlowGraphWalker;
import org.jenkinsci.plugins.workflow.graph.FlowNode;
import org.jenkinsci.plugins.workflow.job.WorkflowRun;
import org.jenkinsci.plugins.workflow.support.actions.PauseAction;
import org.jenkinsci.plugins.workflow.support.steps.StageStep;

/** Progress budgets use execution time of top-level stages, never time before those stages. */
public record PipelineProgress(double percentage, boolean indeterminate, List<Double> boundaries) {
    public PipelineProgress {
        boundaries = List.copyOf(boundaries);
    }

    public record Stage(String key, long duration, boolean completed) {}

    public static PipelineProgress of(WorkflowRun run, long now) {
        List<Stage> current = stages(run, now);
        List<List<Stage>> history = new ArrayList<>();
        WorkflowRun previous = run.getPreviousCompletedBuild();
        for (int count = 0; previous != null && count < 5; count++) {
            history.add(stages(previous, now));
            previous = previous.getPreviousCompletedBuild();
        }
        return calculate(current, history);
    }

    static PipelineProgress calculate(List<Stage> current, List<List<Stage>> history) {
        List<Stage> template =
                history.stream().max(Comparator.comparingInt(List::size)).orElse(List.of());
        Map<String, List<Long>> samples = new LinkedHashMap<>();
        for (Stage stage : template) {
            samples.put(stage.key(), new ArrayList<>());
        }
        for (List<Stage> run : history) {
            for (Stage stage : run) {
                if (stage.completed()) {
                    samples.computeIfAbsent(stage.key(), key -> new ArrayList<>())
                            .add(stage.duration());
                }
            }
        }
        for (Stage stage : current) {
            samples.computeIfAbsent(stage.key(), key -> new ArrayList<>());
        }
        Map<String, Stage> runningStages = new LinkedHashMap<>();
        for (Stage stage : current) {
            runningStages.put(stage.key(), stage);
        }
        Map<String, Double> estimates = new LinkedHashMap<>();
        for (var entry : samples.entrySet()) {
            List<Long> durations = entry.getValue();
            Stage stage = runningStages.get(entry.getKey());
            if (durations.isEmpty()) {
                if (stage == null || stage.completed() == false) {
                    return new PipelineProgress(0, true, List.of());
                }
                durations = List.of(stage.duration());
            }
            // TimingAction has millisecond resolution, including instantaneous stages.
            estimates.put(
                    entry.getKey(),
                    Math.max(
                            1,
                            durations.stream()
                                    .mapToLong(Long::longValue)
                                    .average()
                                    .orElseThrow()));
        }
        double total =
                estimates.values().stream().mapToDouble(Double::doubleValue).sum();
        if (total == 0) {
            return new PipelineProgress(0, true, List.of());
        }
        double elapsed = 0;
        double cumulative = 0;
        List<Double> boundaries = new ArrayList<>();
        for (var entry : estimates.entrySet()) {
            double estimate = entry.getValue();
            Stage stage = runningStages.get(entry.getKey());
            if (stage != null) {
                elapsed += stage.completed() ? estimate : Math.min(stage.duration(), estimate);
            }
            cumulative += estimate;
            if (boundaries.size() < estimates.size() - 1) {
                boundaries.add(cumulative / total * 100);
            }
        }
        return new PipelineProgress(Math.min(100, elapsed / total * 100), current.isEmpty(), boundaries);
    }

    private static List<Stage> stages(WorkflowRun run, long now) {
        FlowExecution execution = run.getExecution();
        if (execution == null) {
            return List.of();
        }
        List<FlowNode> nodes = new ArrayList<>();
        new FlowGraphWalker(execution).forEach(nodes::add);
        List<StepStartNode> starts = nodes.stream()
                .filter(PipelineProgress::isStage)
                .map(StepStartNode.class::cast)
                .filter(node -> node.getEnclosingBlocks().stream().noneMatch(PipelineProgress::isStage))
                .sorted(Comparator.comparingLong((StepStartNode node) -> TimingAction.getStartTime(node))
                        .thenComparingLong(node -> Long.parseLong(node.getId())))
                .toList();
        List<Stage> result = new ArrayList<>();
        Map<String, Integer> occurrences = new LinkedHashMap<>();
        for (StepStartNode start : starts) {
            LabelAction label = start.getAction(LabelAction.class);
            String name = label == null ? start.getDisplayName() : label.getDisplayName();
            String key = name + "#" + occurrences.merge(name, 1, Integer::sum);
            BlockEndNode<?> end = start.getEndNode();
            long from = TimingAction.getStartTime(start);
            long until = end == null ? now : TimingAction.getStartTime(end);
            if (from <= 0 || until < from) {
                continue;
            }
            List<long[]> pauses = new ArrayList<>();
            for (FlowNode node : nodes) {
                if (node.equals(start) || node.getEnclosingBlocks().contains(start)) {
                    for (PauseAction pause : PauseAction.getPauseActions(node)) {
                        long to = pause.getEndTime() == 0 ? until : pause.getEndTime();
                        pauses.add(new long[] {Math.max(from, pause.getStartTime()), Math.min(until, to)});
                    }
                }
            }
            result.add(new Stage(key, activeDuration(from, until, pauses), end != null));
        }
        return List.copyOf(result);
    }

    static long activeDuration(long from, long until, List<long[]> pauses) {
        long paused = 0;
        long cursor = from;
        for (long[] pause : pauses.stream()
                .sorted(Comparator.comparingLong(interval -> interval[0]))
                .toList()) {
            long start = Math.max(cursor, Math.max(from, pause[0]));
            long end = Math.min(until, pause[1]);
            if (end > start) {
                paused += end - start;
                cursor = end;
            }
        }
        return Math.max(0, until - from - paused);
    }

    private static boolean isStage(FlowNode node) {
        return node instanceof StepStartNode start
                && start.getDescriptor() != null
                && start.getDescriptor().isSubTypeOf(StageStep.class);
    }
}

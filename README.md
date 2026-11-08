# GroupSolver Build Board

[![Gitter](https://badges.gitter.im/jenkinsci/ux-sig.svg)](https://gitter.im/jenkinsci/ux-sig?utm_source=badge&utm_medium=badge&utm_campaign=pr-badge)
[![Jenkins Plugin](https://img.shields.io/jenkins/plugin/v/build-monitor-plugin.svg)](https://plugins.jenkins.io/build-monitor-plugin)
[![Jenkins Plugin Installs](https://img.shields.io/jenkins/plugin/i/build-monitor-plugin.svg?color=blue)](https://plugins.jenkins.io/build-monitor-plugin)

## Introduction

Build Monitor Plugin provides a highly visible view of the status of selected Jenkins jobs.

It easily accommodates different computer screen sizes and is ideal as an Extreme Feedback Device to be displayed on a screen on your office wall.

Have a question? Perhaps it's already been answered - check the [FAQ](#faq) section below.

## Features

* Displays the status and progress of selected jobs. By default, the view refreshes every 4 seconds. This can be configured.
* Displays the names of people who might be responsible for "breaking the build".
* Supports [Claim plugin](https://plugins.jenkins.io/claim/), so that you can see who's fixing a broken build.
* Supports [View Job Filters](https://plugins.jenkins.io/view-job-filters/), so that you can easily create Build Monitors for "slow builds", "only failing", etc.
* Supports [Build Failure Analyzer](https://plugins.jenkins.io/build-failure-analyzer/), so that you know not only *who*, but also *what* broke the build.
* Supports [Folder plugin](https://plugins.jenkins.io/cloudbees-folder/), so that you can have project- and team-specific nested Build Monitors.
* Supports [Pipeline plugin](https://plugins.jenkins.io/workflow-aggregator/), so that the currently executing stage(s) are shown.
* Supports [Theme Manager plugin](https://plugins.jenkins.io/theme-manager/), so you can easily switch between different themes.
* Supports [Customizable Header plugin](https://plugins.jenkins.io/customizable-header/), so your team can have a custom header on your Build Monitor.
* The number of columns and size of the font used is easily customisable, making it trivial to accommodate screens of different sizes.
* UI is configurable, you can have a different number of columns _and_ use a different font size on each of the screens in your office.
* Can work in a color-blind-friendly mode.

## Getting started

To create a new Build Monitor View, click on the "New View" tab, select "Build Monitor View" and select jobs you wish to display on the monitor.

Simple, right? :-) You can have as many Build Monitor Views as you want - the most popular approach is to have one per team or one per project.

## Result freshness

Each scheduled job fades from opacity 1.0 after its last completed result to 0.2 at its next timer occurrence. The interval is divided into eight equal parts, dropping opacity by 0.1 at each boundary. Overdue results stay at 0.2 until a new result arrives. Freshness updates with the normal board refresh, using the Jenkins controller clock. Running jobs stay fully visible; jobs without a completed result or a predictable timer schedule retain their normal appearance.

The deadline is the first timer occurrence after completion, including Jenkins `H` hashing, multiline schedules and `TZ=` time zones. It remains anchored to that result so a missed run cannot make an old result brighter again. Job schedules and build execution are unchanged.

## Grouped TV boards

The plugin ID is `groupsolver-build-board`, its Java namespace is `com.groupsolver.jenkins.plugins.buildboard`, and its view type is **GroupSolver Build Board**. It installs alongside the official Build Monitor plugin; existing views, classes, resources, and browser preferences remain separate. Third-party dependency versions are unchanged.

New boards default to text size **0.5**, **6 columns**, and **6 rows**. Existing saved view settings and per-screen preferences remain configurable.

This fork adds these configurable settings:

* **Grouping regex**: a JavaScript regular expression without `/` delimiters, matched against the original full display name. The first participating capture supplies the group name and its capture position supplies group priority; without captures, the full match supplies the name. Groups with the same priority appear in first-seen order from the existing job ordering. Unmatched jobs appear in **Other** at the end. Leave empty to disable grouping.
* **Strip job name regex**: removes every matching substring from tile headings. Job names, URLs, ordering within groups, and build data remain unchanged. Leave empty to show original names.
* **Rows per screen**: a positive integer fixes the number of rows; columns × rows gives the screen's slot capacity. Leave the view field empty for viewport-based row sizing. The settings menu also provides a rows slider alongside the original column and text-size controls; zero selects automatic sizing. Per-screen row preferences persist and reset to the view's setting.

Each group starts at the first column of a fresh row. A partial group row keeps its remaining slots empty; subsequent groups can use the remaining rows on that screen. Overflow continues on the next screen, repeating the group heading when necessary. Existing swipe/scroll navigation, page buttons, and job refresh timing apply. There is no automatic page rotation.

The board fills the screen without the Jenkins header. Only the rounded settings button remains in its upper-right position, at 95% transparency until hovered, keyboard-focused, or opened.

For `KE_OFFICE_TV_BE`, `_(?:(PROD)|(DEV))_` derives `PROD` first and `DEV` second from capture positions, independent of job order. A broader expression such as `^TEST_([^_]+)_` derives any environment name. There is no fixed list of groups or environment-specific ordering. Rows and columns are configurable; four columns and three rows are one possible layout.

Use this stripping expression to display `graphql_logic` for `Tests » TEST_DEV_graphql_logic` (also accepts `>>` and `/` folder separators):

```regex
^(?:Tests\s*(?:»|>>|/)\s*)?TEST_(?:PROD|DEV)_
```

Verify coexistence with the official plugin using `mvn -Dtest=PluginCoexistenceIT -DofficialBuildMonitorArchive=/absolute/path/build-monitor-plugin.hpi test`. The fixture must be the age-eligible, checksum-verified official HPI. This test loads both plugins, round-trips both view types, reloads Jenkins, and checks their class loaders and descriptors remain distinct.

Build the Jenkins plugin with `mvn verify`; the installable artifact is `target/groupsolver-build-board.hpi`. Frontend checks are `npm test`, `./node_modules/.bin/eslint src/main/frontend/`, `./node_modules/.bin/tsc --noEmit`, and `npm run build`. On Node 24, run tests with `NODE_OPTIONS=--no-experimental-webstorage npm test` so jsdom provides browser storage.

# A picture is worth a thousand words

![Adding jobs](docs/empty.png)
![Three columns view](docs/jobs.png)
![Colour-blind mode](docs/color-blind-mode.png)

## FAQ

### How do I let my teammates know that I'm fixing a broken build?

By claiming it. Build Monitor supports Jenkins [Claim Plugin](https://plugins.jenkins.io/claim/), so once you have it installed, enable "Broken build claiming" in the "Post-build actions" of your Jenkins job. From now on you'll be able to claim any further broken builds and Build Monitor will pick it up.

You might also be interested in a [script](https://wiki.jenkins.io/display/JENKINS/Allow+broken+build+claiming+on+every+jobs) that enables claiming on all your Jenkins jobs.

### How do I know what broke the build?

Wouldn't it be great to know _what_ made your build fail? Well of course it would. Build Monitor supports Jenkins [Build Failure Analyzer Plugin](https://plugins.jenkins.io/build-failure-analyzer/) so get it, teach it, and Build Monitor will tell you what the Failure Analyzer found out.

### I have too many jobs on one screen, what should I do?

You have several options here:

1. **review the granularity of your jobs**: If you have too many low-level jobs displayed on one screen, consider consolidating them using [Promoted Builds Plugin](https://plugins.jenkins.io/promoted-builds/) or [MultiJob Plugin](https://plugins.jenkins.io/jenkins-multijob-plugin/)
1. **use job filters**: Build Monitor supports Jenkins [View Job Filters Plugin](https://plugins.jenkins.io/view-job-filters/), if you have it installed
1. get a bigger screen ...

## Your feedback matters!

Found a bug or want to give feedback? Raise [an issue](https://github.com/jenkinsci/build-monitor-plugin/issues?state=open)
or submit a pull request ([start with this mini-dev guide](https://github.com/jenkinsci/build-monitor-plugin/wiki/Development-Guide), it might come in handy).

Build Monitor is continuously delivered to a Jenkins near you thanks to the time and commitment of [the author](http://smartcodeltd.co.uk/) and [the contributors](https://github.com/jenkinsci/build-monitor-plugin/graphs/contributors).

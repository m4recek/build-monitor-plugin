import { fireEvent, render, screen } from "@testing-library/react";
import { beforeEach, describe, expect, it, vi } from "vitest";

import { UserPreferencesProvider } from "../context/user-preference-provider.tsx";
import { Job } from "../models/job.ts";
import PagedGrid from "./paged-grid.tsx";

const scrollTo = vi.fn();

beforeEach(() => {
  window.localStorage.clear();
  scrollTo.mockClear();
  Object.defineProperty(HTMLElement.prototype, "scrollTo", {
    value: scrollTo,
    configurable: true,
  });
  Object.defineProperty(HTMLElement.prototype, "clientWidth", {
    get: () => 1000,
    configurable: true,
  });
});

const makeJob = (name: string): Job => ({
  name,
  url: `/job/${name}/`,
  targetUrl: `/job/${name}/1/console`,
  status: "successful",
  disabled: false,
  hashCode: 1,
  progress: 0,
  resultOpacity: 1,
  estimatedDuration: "",
  config: { displayBadges: "Never" },
  headline: "",
  lastCompletedBuild: {
    name: "",
    url: "",
    duration: "",
    description: "",
    timeElapsedSince: 0,
  },
  currentBuilds: [],
});

const board = (jobs: Job[]) => (
  <UserPreferencesProvider
    monitorId="test"
    defaultPreferences={{
      textSize: 1,
      maximumNumberOfColumns: 2,
      maximumNumberOfRows: 2,
      colorBlindMode: false,
      showBadges: false,
    }}
  >
    <PagedGrid
      jobs={jobs}
      textSize={1}
      maximumNumberOfColumns={2}
      maxRows={2}
      groupPattern={/_(PROD|DEV)_/}
      nameStripPattern={/^Tests\s*»\s*TEST_(?:PROD|DEV)_/g}
    />
  </UserPreferencesProvider>
);

describe("paged board", () => {
  it("strips labels while preserving URLs, source names, group rows and partial-row slots", () => {
    const input = [
      makeJob("Tests » TEST_PROD_graphql_logic"),
      makeJob("Tests » TEST_DEV_solver_logic"),
    ];
    input[0].targetUrl = `${input[0].url}1/HTML_20Report/`;
    input[0].lastCompletedBuild.name = "#1";
    input[0].lastCompletedBuild.timeElapsedSince = 60;
    const { container } = render(board(input));
    expect(
      screen.getByRole("heading", { name: "graphql_logic" }).closest("a"),
    ).toHaveAttribute("href", input[0].targetUrl);
    expect(
      screen.getByRole("heading", { name: "solver_logic" }).closest("a"),
    ).toHaveAttribute("href", input[1].targetUrl);
    expect(screen.getByText("#1").closest("a")).toHaveAttribute(
      "href",
      input[0].targetUrl,
    );
    expect(container.querySelectorAll(".bm-cell a")).toHaveLength(0);
    expect(input[0].url).toBe("/job/Tests » TEST_PROD_graphql_logic/");
    expect(input[0].name).toBe("Tests » TEST_PROD_graphql_logic");
    expect(container.querySelectorAll(".bm-grid-row")).toHaveLength(2);
    expect(
      container.querySelectorAll(".bm-grid-row__jobs")[0].children,
    ).toHaveLength(1);
    expect(container.querySelector(".bm-grid-row__jobs")).toHaveStyle({
      gridTemplateColumns: "minmax(0, 1fr) minmax(0, 1fr)",
    });
    expect(screen.queryByLabelText("Build monitor pages")).toBeNull();
  });

  it("applies server-clock freshness and restores full opacity after a new result", () => {
    const job = makeJob("TEST_PROD_graphql_logic");
    job.resultOpacity = 0.2;
    const view = render(board([job]));
    expect(view.container.querySelector(".bm-cell")).toHaveStyle({
      opacity: "0.2",
    });
    view.rerender(board([{ ...job, resultOpacity: 0.6 }]));
    expect(view.container.querySelector(".bm-cell")).toHaveStyle({
      opacity: "0.6",
    });
    view.rerender(board([{ ...job, resultOpacity: 1 }]));
    expect(view.container.querySelector(".bm-cell")).toHaveStyle({
      opacity: "1",
    });
    view.rerender(board([{ ...job, disabled: true }]));
    expect(
      (view.container.querySelector(".bm-cell") as HTMLElement).style.opacity,
    ).toBe("");
  });

  it("navigates, clamps the active page after refresh, and realigns after resize", () => {
    const input = Array.from({ length: 5 }, (_, index) =>
      makeJob(`Tests » TEST_PROD_job_${index}`),
    );
    const { container, rerender } = render(board(input));
    fireEvent.click(screen.getByRole("button", { name: "Go to page 2" }));
    expect(
      screen.getByRole("button", { name: "Go to page 2" }),
    ).toHaveAttribute("aria-current", "page");
    expect(scrollTo).toHaveBeenLastCalledWith({
      left: 1000,
      behavior: "smooth",
    });
    Object.defineProperty(HTMLElement.prototype, "clientWidth", {
      get: () => 800,
      configurable: true,
    });
    fireEvent(window, new Event("resize"));
    expect(scrollTo).toHaveBeenLastCalledWith({ left: 800, behavior: "auto" });
    rerender(board(input.slice(0, 1)));
    expect(container.querySelectorAll(".bm-grid-page")).toHaveLength(1);
    expect(scrollTo).toHaveBeenLastCalledWith({ left: 0, behavior: "auto" });
  });

  it("keeps manual scroll navigation and group headings on continuation screens", () => {
    const input = Array.from({ length: 5 }, (_, index) =>
      makeJob(`Tests » TEST_PROD_job_${index}`),
    );
    const { container } = render(board(input));
    expect(screen.getAllByText("PROD")).toHaveLength(2);
    const viewport = container.querySelector(
      ".bm-grid-viewport",
    ) as HTMLElement;
    fireEvent.scroll(viewport, { target: { scrollLeft: 1000 } });
    expect(
      screen.getByRole("button", { name: "Go to page 2" }),
    ).toHaveAttribute("aria-current", "page");
  });
});

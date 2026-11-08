import {
  act,
  createEvent,
  fireEvent,
  render,
  screen,
} from "@testing-library/react";
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
  previousBuilds: [],
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
    input[0].targetUrl = `${input[0].url}1/HTML_20Report/index.html`;
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

  it("opens report and console destinations in a new tab on Shift-click", () => {
    vi.useFakeTimers();
    const jobs = [makeJob("TEST_PROD_report"), makeJob("TEST_DEV_console")];
    jobs[0].targetUrl = `${jobs[0].url}1/HTML_20Report/index.html`;
    const open = vi.spyOn(window, "open").mockReturnValue(null);
    const { container } = render(board(jobs));
    const tiles = container.querySelectorAll("a.bm-cell");
    for (const [index, tile] of tiles.entries()) {
      const click = createEvent.click(tile, { shiftKey: true });
      fireEvent(tile, click);
      act(() => vi.runOnlyPendingTimers());
      expect(click.defaultPrevented).toBe(true);
      expect(open).toHaveBeenNthCalledWith(
        index + 1,
        new URL(jobs[index].targetUrl, window.location.href).href,
        "_blank",
        "noopener",
      );
    }
    open.mockRestore();
    vi.useRealTimers();
  });

  it("shows previous runs on right-click and opens the selected run in a new tab", () => {
    const job = makeJob("TEST_PROD_history");
    job.previousBuilds = Array.from({ length: 5 }, (_, index) => ({
      name: `#${6 - index}`,
      targetUrl: `${job.url}${6 - index}/HTML_20Report/index.html`,
    }));
    const open = vi.spyOn(window, "open").mockReturnValue(null);
    const { container } = render(board([job]));
    const tile = container.querySelector("a.bm-cell") as HTMLElement;
    fireEvent.contextMenu(tile, { clientX: 100, clientY: 100 });
    expect(screen.getByRole("menu")).toBeVisible();
    expect(screen.getAllByRole("menuitem")).toHaveLength(5);
    const previous = screen.getByRole("menuitem", { name: "#4" });
    expect(previous).toHaveAttribute("target", "_blank");
    fireEvent.click(previous);
    expect(open).toHaveBeenCalledWith(
      new URL(job.previousBuilds[2].targetUrl, window.location.href).href,
      "_blank",
      "noopener",
    );
    expect(screen.queryByRole("menu")).toBeNull();
    fireEvent.contextMenu(tile);
    fireEvent.keyDown(document, { key: "Escape" });
    expect(screen.queryByRole("menu")).toBeNull();
    expect(tile).toHaveFocus();
    open.mockRestore();
  });

  it("opens history after a long touch and cancels it when the finger moves", () => {
    vi.useFakeTimers();
    const { container } = render(board([makeJob("TEST_PROD_history")]));
    const tile = container.querySelector("a.bm-cell") as HTMLElement;
    const down = createEvent.pointerDown(tile, { clientX: 30, clientY: 30 });
    Object.defineProperty(down, "pointerType", { value: "touch" });
    fireEvent(tile, down);
    act(() => vi.advanceTimersByTime(500));
    expect(
      screen.getByRole("menuitem", { name: "No previous runs" }),
    ).toBeVisible();
    fireEvent.pointerUp(tile);
    const click = createEvent.click(tile);
    fireEvent(tile, click);
    expect(click.defaultPrevented).toBe(true);
    fireEvent.keyDown(document, { key: "Escape" });
    fireEvent(tile, down);
    fireEvent.pointerMove(tile, { clientX: 100, clientY: 100 });
    act(() => vi.advanceTimersByTime(600));
    expect(screen.queryByRole("menu")).toBeNull();
    vi.useRealTimers();
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

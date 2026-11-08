import {
  cleanup,
  fireEvent,
  render,
  screen,
  waitFor,
} from "@testing-library/react";
import { afterEach, beforeEach, describe, expect, it, vi } from "vitest";

import { UseConfirmationProvider } from "../context/confirmation-provider.tsx";
import { JobsProvider } from "../context/jobs-provider.tsx";
import { UseNotificationProvider } from "../context/notification-provider.tsx";
import { UserPreferencesProvider } from "../context/user-preference-provider.tsx";
import Container from "./container.tsx";

beforeEach(() => {
  window.localStorage.clear();
  document.body.innerHTML = '<header class="jenkins-header__actions"></header>';
  Object.defineProperty(HTMLElement.prototype, "scrollTo", {
    value: vi.fn(),
    configurable: true,
  });
  vi.stubGlobal(
    "fetch",
    vi.fn().mockResolvedValue({
      ok: true,
      json: async () => ({
        data: Array.from({ length: 5 }, (_, index) => ({
          name: `Tests » TEST_DEV_job_${index}`,
          url: `/job/${index}/`,
          status: "successful",
          progress: 0,
          estimatedDuration: "",
          config: { displayBadges: "Never" },
          headline: "",
          lastCompletedBuild: { name: "", timeElapsedSince: 0 },
          currentBuilds: [],
        })),
      }),
    }),
  );
});

afterEach(() => {
  cleanup();
  vi.unstubAllGlobals();
});

const board = (
  groupRegex = "_(PROD|DEV)_",
  nameStripRegex = "^Tests\\s*»\\s*TEST_DEV_",
) => (
  <UseNotificationProvider>
    <UseConfirmationProvider>
      <UserPreferencesProvider
        monitorId="TV"
        defaultPreferences={{
          textSize: 1,
          maximumNumberOfColumns: 2,
          maximumNumberOfRows: 2,
          colorBlindMode: false,
          showBadges: false,
        }}
      >
        <JobsProvider autoRefreshEvery={4}>
          <Container groupRegex={groupRegex} nameStripRegex={nameStripRegex} />
        </JobsProvider>
      </UserPreferencesProvider>
    </UseConfirmationProvider>
  </UseNotificationProvider>
);

describe("board controls", () => {
  it("changes page capacity through the rows slider and retains it after remount", async () => {
    const first = render(board());
    await screen.findByRole("heading", { name: "job_0" });
    expect(first.container.querySelectorAll(".bm-grid-page")).toHaveLength(2);
    fireEvent.click(
      document.querySelector(
        ".jenkins-header__actions button",
      ) as HTMLButtonElement,
    );
    fireEvent.change(
      await screen.findByRole("slider", {
        name: "Rows per screen (0 = automatic)",
      }),
      { target: { value: "3" } },
    );
    expect(first.container.querySelectorAll(".bm-grid-page")).toHaveLength(1);
    expect(window.localStorage.getItem("buildMonitor.TV.numberOfRows")).toBe(
      "3",
    );
    first.unmount();
    const second = render(board());
    await screen.findByRole("heading", { name: "job_0" });
    expect(second.container.querySelectorAll(".bm-grid-page")).toHaveLength(1);
  });

  it("reports malformed grouping and stripping regexes and recovers when corrected", async () => {
    const view = render(board("["));
    expect(screen.getByRole("alert")).toHaveTextContent(
      "Invalid board configuration",
    );
    expect(screen.getByRole("link", { name: "Edit view" })).toHaveAttribute(
      "href",
      "configure",
    );
    view.rerender(board("_(PROD|DEV)_", "["));
    expect(screen.getByRole("alert")).toHaveTextContent(
      "Invalid board configuration",
    );
    view.rerender(board());
    await screen.findByRole("heading", { name: "job_0" });
    await waitFor(() => expect(screen.queryByRole("alert")).toBeNull());
  });
});

import { fireEvent, render, screen } from "@testing-library/react";
import { beforeEach, describe, expect, it } from "vitest";

import {
  UserPreferencesProvider,
  useUserPreferences,
} from "./user-preference-provider.tsx";

function RowControls() {
  const { maximumNumberOfRows, setMaximumNumberOfRows, reset, isResettable } =
    useUserPreferences();
  return (
    <>
      <output aria-label="Rows">{maximumNumberOfRows}</output>
      <button onClick={() => setMaximumNumberOfRows(5)}>Five rows</button>
      <button onClick={() => setMaximumNumberOfRows(0)}>Automatic rows</button>
      <button onClick={reset} disabled={isResettable}>
        Reset
      </button>
    </>
  );
}

const preferences = (idMonitor: string) => (
  <UserPreferencesProvider
    monitorId={idMonitor}
    defaultPreferences={{
      textSize: 1,
      maximumNumberOfColumns: 4,
      maximumNumberOfRows: 3,
      colorBlindMode: false,
      showBadges: true,
    }}
  >
    <RowControls />
  </UserPreferencesProvider>
);

beforeEach(() => window.localStorage.clear());

describe("row preferences", () => {
  it("persists rows across remounts and resets to the configured view value", () => {
    const first = render(preferences("TV"));
    expect(screen.getByLabelText("Rows")).toHaveTextContent("3");
    expect(screen.getByText("Reset")).toBeDisabled();
    fireEvent.click(screen.getByText("Five rows"));
    expect(screen.getByLabelText("Rows")).toHaveTextContent("5");
    expect(screen.getByText("Reset")).toBeEnabled();
    first.unmount();

    render(preferences("TV"));
    expect(screen.getByLabelText("Rows")).toHaveTextContent("5");
    fireEvent.click(screen.getByText("Reset"));
    expect(screen.getByLabelText("Rows")).toHaveTextContent("3");
    expect(window.localStorage.getItem("buildMonitor.TV.numberOfRows")).toBe(
      "3",
    );
  });

  it("persists automatic sizing independently for each board", () => {
    const first = render(preferences("TV"));
    fireEvent.click(screen.getByText("Automatic rows"));
    expect(window.localStorage.getItem("buildMonitor.TV.numberOfRows")).toBe(
      "0",
    );
    first.unmount();
    const second = render(preferences("Other"));
    expect(screen.getByLabelText("Rows")).toHaveTextContent("3");
    second.unmount();
    render(preferences("TV"));
    expect(screen.getByLabelText("Rows")).toHaveTextContent("0");
  });
});

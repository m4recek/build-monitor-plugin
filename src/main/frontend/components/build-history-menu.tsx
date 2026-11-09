import "./build-history-menu.scss";

import { useLayoutEffect, useRef, useState } from "react";
import { createPortal } from "react-dom";

import { Job, JobStatus } from "../models/job.ts";
import openInNewTab from "../utils/open-in-new-tab.ts";
import { EXTERNAL_LINK_SYMBOL } from "../utils/symbols.tsx";

const buildTimeFormat = new Intl.DateTimeFormat(undefined, {
  dateStyle: "medium",
  timeStyle: "short",
});

const resultLabels: Record<JobStatus, string> = {
  successful: "Passed",
  failing: "Failed",
  unstable: "Unstable",
  aborted: "Aborted",
  unknown: "Not built",
  running: "Running",
};

export default function BuildHistoryMenu({
  name,
  jobUrl,
  builds,
  point,
  onClose,
}: {
  name: string;
  jobUrl: string;
  builds: Job["previousBuilds"];
  point: { x: number; y: number };
  onClose: (restoreFocus?: boolean) => void;
}) {
  const menu = useRef<HTMLDivElement>(null);
  const [position, setPosition] = useState(point);

  useLayoutEffect(() => {
    const element = menu.current;
    if (element === null) {
      return;
    }
    const bounds = element.getBoundingClientRect();
    setPosition({
      x: Math.max(8, Math.min(point.x, window.innerWidth - bounds.width - 8)),
      y: Math.max(8, Math.min(point.y, window.innerHeight - bounds.height - 8)),
    });
    (
      element.querySelector<HTMLElement>(".bm-build-history__details") ??
      element
    ).focus();
    const outside = (event: PointerEvent) => {
      if (element.contains(event.target as Node) === false) {
        onClose();
      }
    };
    const keyboard = (event: KeyboardEvent) => {
      if (event.key === "Escape" || event.key === "Tab") {
        onClose(event.key === "Escape");
      }
    };
    const resize = () => onClose();
    document.addEventListener("pointerdown", outside);
    document.addEventListener("keydown", keyboard);
    window.addEventListener("resize", resize);
    return () => {
      document.removeEventListener("pointerdown", outside);
      document.removeEventListener("keydown", keyboard);
      window.removeEventListener("resize", resize);
    };
  }, [point.x, point.y, onClose]);

  return createPortal(
    <div
      ref={menu}
      role="menu"
      aria-label={`Previous runs of ${name}`}
      tabIndex={-1}
      className="bm-build-history"
      style={{ left: position.x, top: position.y }}
      onClick={(event) => {
        const link = (event.target as Element).closest<HTMLAnchorElement>("a");
        if (link === null || event.metaKey || event.ctrlKey) {
          return;
        }
        event.preventDefault();
        openInNewTab(link.href);
        onClose();
      }}
      onKeyDown={(event) => {
        const items = Array.from(event.currentTarget.querySelectorAll("a"));
        const index = items.indexOf(
          document.activeElement as HTMLAnchorElement,
        );
        if (
          items.length > 0 &&
          (event.key === "ArrowDown" || event.key === "ArrowUp")
        ) {
          event.preventDefault();
          const direction = event.key === "ArrowDown" ? 1 : -1;
          items[(index + direction + items.length) % items.length].focus();
        }
      }}
    >
      <div className="bm-build-history__heading">
        <span>Previous runs</span>
        <a
          role="menuitem"
          className="bm-build-history__icon"
          href={jobUrl}
          target="_blank"
          rel="noopener noreferrer"
          aria-label={`Open job page for ${name}`}
          title="Open job page"
        >
          {EXTERNAL_LINK_SYMBOL}
        </a>
      </div>
      {builds.length === 0 && (
        <div role="menuitem" aria-disabled="true">
          No previous runs
        </div>
      )}
      {builds.map((build) => (
        <div
          role="none"
          className={`bm-build-history__run bm-build-history__run--${build.status}`}
          key={build.targetUrl}
        >
          <a
            role="menuitem"
            className="bm-build-history__details"
            href={build.targetUrl}
            target="_blank"
            rel="noopener noreferrer"
            aria-label={`${build.name} ${resultLabels[build.status]}`}
          >
            <span className="bm-build-history__summary">
              <span>{build.name}</span>
              <span className="bm-build-history__result">
                {resultLabels[build.status]}
              </span>
            </span>
            <span className="bm-build-history__timing">
              <time dateTime={new Date(build.startedAt).toISOString()}>
                {buildTimeFormat.format(build.startedAt)}
              </time>
              <span title="Build duration">{build.duration}</span>
            </span>
          </a>
          <a
            role="menuitem"
            className="bm-build-history__icon"
            href={build.url}
            target="_blank"
            rel="noopener noreferrer"
            aria-label={`Open build ${build.name} page`}
            title={`Open build ${build.name} page`}
          >
            {EXTERNAL_LINK_SYMBOL}
          </a>
        </div>
      ))}
    </div>,
    document.body,
  );
}

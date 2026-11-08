import "./build-history-menu.scss";

import { useLayoutEffect, useRef, useState } from "react";
import { createPortal } from "react-dom";

import { Job } from "../models/job.ts";

export default function BuildHistoryMenu({
  name,
  builds,
  point,
  onClose,
}: {
  name: string;
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
    (element.querySelector<HTMLElement>("a") ?? element).focus();
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
      <div className="bm-build-history__heading">Previous runs</div>
      {builds.length === 0 && (
        <div role="menuitem" aria-disabled="true">
          No previous runs
        </div>
      )}
      {builds.map((build) => (
        <a
          role="menuitem"
          key={build.targetUrl}
          href={build.targetUrl}
          target="_blank"
          rel="noopener noreferrer"
          onClick={(event) => {
            event.preventDefault();
            window.open(event.currentTarget.href, "_blank", "noopener");
            onClose();
          }}
        >
          {build.name}
        </a>
      ))}
    </div>,
    document.body,
  );
}

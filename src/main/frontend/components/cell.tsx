import { useCallback, useEffect, useRef, useState } from "react";

import { useUserPreferences } from "../context/user-preference-provider.tsx";
import { Job } from "../models/job";
import { buildStatusToClass } from "../utils/utils.ts";
import BuildHistoryMenu from "./build-history-menu.tsx";
import Label from "./label";
import Badges from "./snippets/badges.tsx";
import BuildNumber from "./snippets/build-number.tsx";
import BuildTime from "./snippets/build-time.tsx";
import Claim from "./snippets/claim.tsx";
import Problems from "./snippets/problems.tsx";
import Tests from "./snippets/tests.tsx";

function Cell({
  job,
  nameStripPattern,
}: {
  job: Job;
  nameStripPattern: RegExp | null;
}) {
  const { colorBlindMode } = useUserPreferences();
  const tile = useRef<HTMLAnchorElement>(null);
  const timer = useRef<number | undefined>(undefined);
  const touchStart = useRef({ x: 0, y: 0 });
  const held = useRef(false);
  const [menuPoint, setMenuPoint] = useState<{ x: number; y: number } | null>(
    null,
  );
  const name =
    nameStripPattern === null
      ? job.name
      : job.name.replace(nameStripPattern, "");
  const closeMenu = useCallback((restoreFocus = false) => {
    setMenuPoint(null);
    held.current = false;
    if (restoreFocus) {
      tile.current?.focus();
    }
  }, []);
  const cancelHold = () => window.clearTimeout(timer.current);
  useEffect(() => () => window.clearTimeout(timer.current), []);

  return (
    <>
      <a
        ref={tile}
        aria-haspopup="menu"
        aria-expanded={menuPoint !== null}
        onContextMenu={(event) => {
          event.preventDefault();
          held.current = true;
          cancelHold();
          const bounds = event.currentTarget.getBoundingClientRect();
          const keyboard = event.clientX === 0 && event.clientY === 0;
          setMenuPoint({
            x: keyboard ? bounds.left + bounds.width / 2 : event.clientX,
            y: keyboard ? bounds.top + bounds.height / 2 : event.clientY,
          });
        }}
        onPointerDown={(event) => {
          held.current = false;
          cancelHold();
          if (event.pointerType === "touch" || event.pointerType === "pen") {
            const point = { x: event.clientX, y: event.clientY };
            touchStart.current = point;
            timer.current = window.setTimeout(() => {
              held.current = true;
              setMenuPoint(point);
            }, 500);
          }
        }}
        onPointerMove={(event) => {
          if (
            Math.hypot(
              event.clientX - touchStart.current.x,
              event.clientY - touchStart.current.y,
            ) > 10
          ) {
            cancelHold();
          }
        }}
        onPointerUp={cancelHold}
        onPointerCancel={cancelHold}
        href={job.targetUrl}
        onClick={(event) => {
          if (held.current) {
            event.preventDefault();
            held.current = false;
            return;
          }
          if (event.shiftKey) {
            event.preventDefault();
            const destination = event.currentTarget.href;
            // Leave the Shift-click event before Chrome chooses a tab or window.
            window.setTimeout(
              () => window.open(destination, "_blank", "noopener"),
              0,
            );
          }
        }}
        style={job.disabled ? undefined : { opacity: job.resultOpacity }}
        className={
          "bm-cell bm-cell" +
          buildStatusToClass(job) +
          " " +
          (colorBlindMode ? "bm-cell--color-blind-mode" : "")
        }
      >
        <div
          className={"bm-cell-duration bm-cell-duration--animate"}
          style={{ width: job.progress + "%" }}
        />

        <h2>
          <Label
            text={name}
            style={{
              fontSize: "1.25em",
              fontWeight: "550",
              color:
                "var(--bm-contrast, color-mix(in srgb, var(--bm-cell-color), var(--text-color)))",
            }}
          />
        </h2>

        {job.headline && !job.claim?.active && <Label text={job.headline} />}

        {job.lastCompletedBuild.description && (
          <Label text={job.lastCompletedBuild.description} />
        )}

        <Claim job={job} />
        <Problems job={job} />
        <Tests job={job} />
        <Badges job={job} />

        <div className={"bm-cell__details"}>
          <BuildNumber job={job} />

          <BuildTime job={job} />
        </div>
      </a>
      {menuPoint !== null && (
        <BuildHistoryMenu
          name={name}
          builds={job.previousBuilds}
          point={menuPoint}
          onClose={closeMenu}
        />
      )}
    </>
  );
}

export default Cell;

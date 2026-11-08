import { useEffect, useMemo, useRef, useState } from "react";

import { Job } from "../models/job.ts";
import { createBoardPages } from "../utils/board-layout.ts";
import { getColumnCount, getRowsPerPage } from "../utils/grid-pagination.ts";
import Cell from "./cell.tsx";

const getInitialViewportHeight = () =>
  typeof window === "undefined" ? 0 : window.innerHeight;
const getInitialViewportWidth = () =>
  typeof window === "undefined" ? 0 : window.innerWidth;

interface PagedGridProps {
  jobs: Job[];
  textSize: number;
  maximumNumberOfColumns: number;
  maxRows: number | null;
  groupPattern: RegExp | null;
  nameStripPattern: RegExp | null;
}

function PagedGrid({
  jobs,
  textSize,
  maximumNumberOfColumns,
  maxRows,
  groupPattern,
  nameStripPattern,
}: PagedGridProps) {
  const viewportRef = useRef<HTMLDivElement>(null);
  const [viewportHeight, setViewportHeight] = useState(
    getInitialViewportHeight,
  );
  const [viewportWidth, setViewportWidth] = useState(getInitialViewportWidth);
  const [currentPage, setCurrentPage] = useState(0);

  const columnCount =
    maxRows === null
      ? getColumnCount(jobs.length, maximumNumberOfColumns)
      : maximumNumberOfColumns;
  const rowsPerPage =
    maxRows === null ? getRowsPerPage({ viewportHeight, textSize }) : maxRows;
  const pages = useMemo(
    () => createBoardPages(jobs, groupPattern, columnCount, rowsPerPage),
    [jobs, groupPattern, columnCount, rowsPerPage],
  );

  const scrollToPage = (pageIndex: number, behavior: ScrollBehavior = "smooth") => {
    const viewport = viewportRef.current;
    if (viewport === null) {
      return;
    }

    viewport.scrollTo({
      left: pageIndex * viewport.clientWidth,
      behavior,
    });
  };

  useEffect(() => {
    const viewport = viewportRef.current;
    if (viewport === null) {
      return;
    }

    const updateViewportSize = () => {
      setViewportHeight(viewport.clientHeight);
      setViewportWidth(viewport.clientWidth);
    };

    updateViewportSize();

    if (typeof ResizeObserver === "undefined") {
      window.addEventListener("resize", updateViewportSize);
      return () => window.removeEventListener("resize", updateViewportSize);
    }

    const observer = new ResizeObserver(updateViewportSize);
    observer.observe(viewport);

    return () => observer.disconnect();
  }, []);

  useEffect(() => {
    if (viewportWidth <= 0) {
      return;
    }

    const lastPageIndex = Math.max(pages.length - 1, 0);
    const nextPage = Math.min(currentPage, lastPageIndex);
    if (nextPage !== currentPage) {
      setCurrentPage(nextPage);
    }
    scrollToPage(nextPage, "auto");
  }, [pages.length, viewportWidth]);

  const handleScroll = () => {
    const viewport = viewportRef.current;
    if (viewport === null) {
      return;
    }

    const nextPage = Math.max(
      0,
      Math.min(
        pages.length - 1,
        Math.round(viewport.scrollLeft / Math.max(viewport.clientWidth, 1)),
      ),
    );

    if (nextPage !== currentPage) {
      setCurrentPage(nextPage);
    }
  };

  return (
    <div className="bm-grid-shell">
      <div
        ref={viewportRef}
        className="bm-grid-viewport"
        onScroll={handleScroll}
      >
        <div className="bm-grid-track">
          {pages.map((pageRows, pageIndex) => (
            <section
              key={`page-${pageIndex}`}
              className="bm-grid-page"
              aria-label={`Page ${pageIndex + 1} of ${pages.length}`}
            >
              <div
                className="bm-grid"
                style={{
                  fontSize: textSize + "rem",
                  gridTemplateColumns: "minmax(0, 1fr)",
                  gridTemplateRows: "minmax(0, 1fr) ".repeat(rowsPerPage),
                }}
              >
                {pageRows.map((row, rowIndex) => (
                  <section
                    className="bm-grid-row"
                    key={row.jobs[0].url}
                    aria-label={row.group || undefined}
                  >
                    {row.group !== "" &&
                      (row.startsGroup || rowIndex === 0) && (
                        <div className="bm-grid-row__group">{row.group}</div>
                      )}
                    <div
                      className="bm-grid-row__jobs"
                      style={{
                        gridTemplateColumns: "minmax(0, 1fr) ".repeat(
                          columnCount,
                        ),
                      }}
                    >
                      {row.jobs.map((job) => (
                        <Cell
                          key={job.url}
                          job={job}
                          nameStripPattern={nameStripPattern}
                        />
                      ))}
                    </div>
                  </section>
                ))}
              </div>
            </section>
          ))}
        </div>
      </div>

      {pages.length > 1 && (
        <div className="bm-grid-pagination" aria-label="Build monitor pages">
          {pages.map((_, pageIndex) => (
            <button
              key={`page-dot-${pageIndex}`}
              type="button"
              className={
                "bm-grid-pagination__dot" +
                (currentPage === pageIndex ? " bm-grid-pagination__dot--active" : "")
              }
              aria-label={`Go to page ${pageIndex + 1}`}
              aria-current={
                currentPage === pageIndex ? ("page" as const) : undefined
              }
              onClick={() => {
                setCurrentPage(pageIndex);
                scrollToPage(pageIndex);
              }}
            />
          ))}
        </div>
      )}
    </div>
  );
}

export default PagedGrid;

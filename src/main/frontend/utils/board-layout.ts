import { Job } from "../models/job.ts";
import { paginateItems } from "./grid-pagination.ts";

export interface BoardRow {
  group: string;
  startsGroup: boolean;
  jobs: Job[];
}

export function createBoardPages(
  jobs: Job[],
  groupPattern: RegExp | null,
  columns: number,
  rowsPerPage: number,
): BoardRow[][] {
  if (
    Number.isInteger(columns) === false ||
    columns < 1 ||
    Number.isInteger(rowsPerPage) === false ||
    rowsPerPage < 1
  ) {
    throw new Error("Board columns and rows must be positive integers.");
  }

  const groupedJobs = new Map<string, { jobs: Job[]; priority: number }>();
  const unmatched: Job[] = [];
  for (const job of jobs) {
    const match = groupPattern === null ? null : job.name.match(groupPattern);
    if (groupPattern !== null && match === null) {
      unmatched.push(job);
      continue;
    }
    const captureIndex =
      match?.findIndex((value, index) => index > 0 && value !== undefined) ??
      -1;
    const priority = Math.max(captureIndex, 0);
    const group = match === null ? "" : match[priority];
    const items = groupedJobs.get(group);
    if (items === undefined) {
      groupedJobs.set(group, { jobs: [job], priority });
    } else {
      items.jobs.push(job);
      items.priority = Math.min(items.priority, priority);
    }
  }

  const entries = Array.from(groupedJobs.entries())
    .sort((left, right) => left[1].priority - right[1].priority)
    .map(([group, items]) => [group, items.jobs] as const);
  if (unmatched.length > 0) {
    entries.push(["Other", unmatched]);
  }
  const rows = entries.flatMap(([group, items]) => {
    return paginateItems(items, columns, 1).map((rowJobs, rowIndex) => ({
      group,
      startsGroup: rowIndex === 0,
      jobs: rowJobs,
    }));
  });
  return paginateItems(rows, 1, rowsPerPage);
}

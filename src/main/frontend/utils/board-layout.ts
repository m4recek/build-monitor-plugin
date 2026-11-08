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

  const groupedJobs = new Map<string, Job[]>();
  const unmatched: Job[] = [];
  for (const job of jobs) {
    const match = groupPattern === null ? null : job.name.match(groupPattern);
    if (groupPattern !== null && match === null) {
      unmatched.push(job);
      continue;
    }
    const group = match === null ? "" : (match[1] ?? match[0]);
    const items = groupedJobs.get(group);
    if (items === undefined) {
      groupedJobs.set(group, [job]);
    } else {
      items.push(job);
    }
  }

  const entries = Array.from(groupedJobs.entries());
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

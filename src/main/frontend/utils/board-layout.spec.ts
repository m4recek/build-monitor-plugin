import { describe, expect, it } from "vitest";

import { Job } from "../models/job.ts";
import { createBoardPages } from "./board-layout.ts";

const jobs = (...names: string[]) =>
  names.map((name) => ({ name, url: `/job/${name}/` }) as Job);

describe("board layout", () => {
  it("derives groups from captures in first-seen order and starts each on a new row", () => {
    const input = jobs(
      "TEST_DEV_one",
      "TEST_PROD_one",
      "TEST_PROD_two",
      "TEST_PROD_three",
      "TEST_DEV_two",
    );
    const pages = createBoardPages(input, /_(PROD|DEV)_/, 2, 3);
    expect(pages).toHaveLength(1);
    expect(
      pages[0].map((row) => [row.group, row.jobs.map((job) => job.name)]),
    ).toEqual([
      ["DEV", ["TEST_DEV_one", "TEST_DEV_two"]],
      ["PROD", ["TEST_PROD_one", "TEST_PROD_two"]],
      ["PROD", ["TEST_PROD_three"]],
    ]);
  });

  it("uses participating capture positions to order groups independently of job order", () => {
    const input = jobs(
      "TEST_DEV_one",
      "TEST_PROD_one",
      "other",
      "TEST_PROD_two",
    );
    const pages = createBoardPages(input, /_(?:(PROD)|(DEV))_/, 3, 3);
    expect(
      pages[0].map((row) => [row.group, row.jobs.map((job) => job.name)]),
    ).toEqual([
      ["PROD", ["TEST_PROD_one", "TEST_PROD_two"]],
      ["DEV", ["TEST_DEV_one"]],
      ["Other", ["other"]],
    ]);
    const reversed = createBoardPages(input, /_(?:(DEV)|(PROD))_/, 3, 3);
    expect(reversed[0].map((row) => row.group)).toEqual([
      "DEV",
      "PROD",
      "Other",
    ]);
  });

  it("leaves partial rows empty and continues groups across screens without losing jobs", () => {
    const input = jobs(
      ...Array.from({ length: 9 }, (_, index) => `TEST_PROD_${index}`),
      "TEST_DEV_one",
    );
    const pages = createBoardPages(input, /_(PROD|DEV)_/, 2, 2);
    expect(pages.map((page) => page.map((row) => row.jobs.length))).toEqual([
      [2, 2],
      [2, 2],
      [1, 1],
    ]);
    expect(pages.flat().flatMap((row) => row.jobs)).toEqual(input);
  });

  it("accepts arbitrary captured group names and retains unmatched jobs at the end", () => {
    const input = jobs(
      "TEST_QA_one",
      "other",
      "TEST_STAGE_two",
      "TEST_QA_three",
    );
    const pages = createBoardPages(input, /^TEST_([^_]+)_/, 4, 3);
    expect(pages[0].map((row) => row.group)).toEqual(["QA", "STAGE", "Other"]);
    expect(pages.flat().flatMap((row) => row.jobs)).toEqual([
      input[0],
      input[3],
      input[2],
      input[1],
    ]);
  });

  it("uses the full match when the regex has no capture", () => {
    const pages = createBoardPages(
      jobs("TEST_PROD_one", "TEST_DEV_one"),
      /PROD|DEV/,
      4,
      2,
    );
    expect(pages[0].map((row) => row.group)).toEqual(["PROD", "DEV"]);
  });

  it("paginates ungrouped jobs and leaves an empty board empty", () => {
    expect(
      createBoardPages(
        jobs("one", "two", "three", "four", "five"),
        null,
        2,
        2,
      ).map((page) => page.map((row) => row.jobs.length)),
    ).toEqual([[2, 2], [1]]);
    expect(createBoardPages([], null, 2, 2)).toEqual([]);
  });

  it("rejects invalid dimensions", () => {
    for (const invalid of [0, -1, 1.5, NaN, Infinity]) {
      expect(() => createBoardPages([], null, invalid, 2)).toThrow(
        "positive integers",
      );
      expect(() => createBoardPages([], null, 2, invalid)).toThrow(
        "positive integers",
      );
    }
  });
});

import { useJobs } from "../context/jobs-provider.tsx";
import { useUserPreferences } from "../context/user-preference-provider.tsx";
import Notice from "./notice.tsx";
import OptionsButton from "./options-button";
import PagedGrid from "./paged-grid.tsx";

function Container({
  groupRegex,
  nameStripRegex,
}: {
  groupRegex: string;
  nameStripRegex: string;
}) {
  const { jobs, isLoading } = useJobs();
  const { textSize, maximumNumberOfColumns, maximumNumberOfRows } =
    useUserPreferences();

  const settings = (() => {
    try {
      if (
        Number.isInteger(maximumNumberOfRows) === false ||
        maximumNumberOfRows < 0
      ) {
        throw new Error("Rows per screen must be a positive integer.");
      }
      if (
        Number.isInteger(maximumNumberOfColumns) === false ||
        maximumNumberOfColumns < 1
      ) {
        throw new Error("Columns must be a positive integer.");
      }
      return {
        groupPattern: groupRegex === "" ? null : new RegExp(groupRegex),
        nameStripPattern:
          nameStripRegex === "" ? null : new RegExp(nameStripRegex, "g"),
      };
    } catch (error) {
      return { error: String(error) };
    }
  })();

  if ("error" in settings) {
    return (
      <>
        <div role="alert">
          Invalid board configuration: {settings.error}{" "}
          <a href="configure">Edit view</a>
        </div>
        <OptionsButton amountOfJobs={jobs.length} />
      </>
    );
  }

  return (
    <>
      {isLoading === false && (
        <>
          {jobs.length === 0 && <Notice />}
          {jobs.length > 0 && (
            <PagedGrid
              jobs={jobs}
              textSize={textSize}
              maximumNumberOfColumns={maximumNumberOfColumns}
              maxRows={maximumNumberOfRows === 0 ? null : maximumNumberOfRows}
              groupPattern={settings.groupPattern}
              nameStripPattern={settings.nameStripPattern}
            />
          )}
        </>
      )}
      <OptionsButton amountOfJobs={jobs.length} />
    </>
  );
}

export default Container;

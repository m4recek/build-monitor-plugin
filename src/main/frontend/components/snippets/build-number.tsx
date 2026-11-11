import { Job } from "../../models/job.ts";
import Label from "../label.tsx";

export default function BuildNumber({ job }: { job: Job }) {
  const hasCurrentBuilds = job.currentBuilds.length > 0;
  const hasLastCompletedBuild =
    job.lastCompletedBuild?.timeElapsedSince && hasCurrentBuilds === false;

  return (
    <div>
      {hasCurrentBuilds &&
        job.currentBuilds?.map((build, index) => (
          <div className="bm-build-time" key={index}>
            {build.name && (
              <span
                title={`Details of ${job.name}, build ${build.name}`}
              >
                <Label text={build.name} />
              </span>
            )}
            {build.pipelineStages && (
              <>
                {"•"}
                <Label text={build.pipelineStages} />
              </>
            )}
          </div>
        ))}

      {hasLastCompletedBuild && job.lastCompletedBuild?.name && (
        <span
          title={`Details of ${job.name}, build ${job.lastCompletedBuild.name}`}
        >
          <Label text={job.lastCompletedBuild.name} />
        </span>
      )}
    </div>
  );
}

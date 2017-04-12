import net.praqma.quticon.BuildDataEntry
import org.jenkinsci.plugins.workflow.job.WorkflowRun
import org.jenkinsci.plugins.workflow.multibranch.WorkflowMultiBranchProject
// This is dependency to https://github.com/jenkinsci/pipeline-stage-view-plugin
// There is no good reason to extract pipeline stages from WorkflowRun on our own
import com.cloudbees.workflow.rest.external.RunExt

// This method takes a list of job names and a time interval in hours
// and returns list of BuildDataEntry
// NonCPS added to avoid java.io.NotSerializableException's
// Read more here https://github.com/jenkinsci/workflow-cps-plugin#technical-design
@NonCPS
def call(def jobNames, def numberOfHoursBack) {
	def buildResults = []
	if (jobNames==[]) {
		echo "No job names specified for extraction. Check all jobs"
		jobNames = Jenkins.instance.getJobNames()
	}
    	for (def jobName: jobNames) {
		echo "Looking for the job with the name $jobName"
		def jobs = getJobs(jobName)
		def builds = []
		for (def job: jobs) {
			builds += job.getBuilds().byTimestamp(System.currentTimeMillis()-numberOfHoursBack*60*60*1000, System.currentTimeMillis()).completedOnly()
		}
    		echo "Found ${builds.size()} builds matching time criteria for the job ${jobName}"
		for (def build: builds) {
			// If this is a pipeline then extract pipeline stages as separate entries in addition to the pipeline run itself that will
			// be extracted in the next step
			if (build instanceof WorkflowRun) {
				for (flowNode in RunExt.create(build).getStages()) {
					def stage_entry = new BuildDataEntry(job_name: "${jobName}/${flowNode.getName()}", 
			           		   verdict: flowNode.getStatus(),
			                           build_number: build.number,
				                   duration: flowNode.getDurationMillis(), 
				                   timestamp: flowNode.getStartTimeMillis(),
						   time_in_queue: 0) // Fixme!
					echo "New pipeline stage entry: name ${stage_entry.job_name}, result ${stage_entry.verdict}, number ${stage_entry.build_number}, duration ${stage_entry.duration}, timestamp ${stage_entry.timestamp}, time in queue ${stage_entry.time_in_queue}"
            				buildResults.add(stage_entry)
				}
			}
			 def entry = new BuildDataEntry(job_name: jobName, 
			           		   verdict: build.result,
			                           build_number: build.number,
				                   duration: build.duration, 
				                   timestamp: build.getTimeInMillis(),
						   time_in_queue: build.getStartTimeInMillis() - build.getTimeInMillis())
			echo "New entry: name ${entry.job_name}, result ${entry.verdict}, number ${entry.build_number}, duration ${entry.duration}, timestamp ${entry.timestamp}, time in queue ${entry.time_in_queue}"
            		buildResults.add(entry)
        	}
	}
	return buildResults
}

def getJobs(jobName) {
	echo "Looking for the job with the name $jobName"
	def result = []
	def job = Jenkins.instance.getItem(jobName)
	if (job != null && job instanceof WorkflowMultiBranchProject) {
		echo "${jobName} is a MultiBranchProject, return list of child jobs " + job.getAllJobs()
		result = job.getAllJobs()
	}
	if (job != null) {
		result = [job]
	}
	echo "Job ${jobName} wasn't found. One possible option that it is a branch jobs of multibranch pipeline that can't be accessed usual way"
	echo "Check if we are dealing with multibranch pipeline. First check for / in job the name"
	if (jobName.contains("/")) {
		possibleMultiBranchParent = jobName.split("/")[0]
		echo "/ found"
		echo "Check if ${possibleMultiBranchParent} is a multibranch pipeline"
		def possibleMultiBranchParentJob = Jenkins.instance.getItem(possibleMultiBranchParent)
		if (possibleMultiBranchParentJob != null && possibleMultiBranchParentJob instanceof WorkflowMultiBranchProject) {
			echo "It is a multibranch job. Extracted child job " + jobName.split("/")[1]
		        result = [possibleMultiBranchParentJob.getItem(jobName.split("/")[1])]
		} else {
			echo "${possibleMultiBranchParentJob} is either null or not multibranch pipeline. Skip ${jobName} and continue"
			result = []
		}
	} else {
		echo "${jobName} doesn't contain / so we are out of guesses. Check you configuration and make sure that ${jobName} exists. Continue"
		result = []
       	}
	result
}

package net.praqma.quticon

import org.jenkinsci.plugins.workflow.multibranch.WorkflowMultiBranchProject

class Utils implements Serializable {
	def steps
 	Utils(steps) {this.steps = steps}

	def getJobs(jobName) {

		def job = Jenkins.instance.getItem(jobName)

		if (job != null && job instanceof WorkflowMultiBranchProject) {
			steps.echo "${jobName} is a MultiBranchProject, return list of child jobs " + job.getAllJobs()
			return job.getAllJobs()
		}
	
		if (job != null) {
			return [job]
		}
	
		steps.echo "Job ${jobName} wasn't found. One possible option that it is a branch jobs of multibranch pipeline that can't be accessed usual way"
		steps.echo "Check if we are dealing with multibranch pipeline. First check for / in job the name"
		if (jobName.contains("/")) {
			def possibleMultiBranchParent = jobName.split("/")[0]
			steps.echo "/ found"
			steps.echo "Check if ${possibleMultiBranchParent} is a multibranch pipeline"
			def possibleMultiBranchParentJob = Jenkins.instance.getItem(possibleMultiBranchParent)
			if (possibleMultiBranchParentJob != null && possibleMultiBranchParentJob instanceof WorkflowMultiBranchProject) {
				steps.echo "It is a multibranch job. Extracted child job " + jobName.split("/")[1]
		        	return [possibleMultiBranchParentJob.getItem(jobName.split("/")[1])]
			} else {
				steps.echo "${possibleMultiBranchParentJob} is either null or not multibranch pipeline. Skip ${jobName} and continue"
				return []
			}
		} else {
			steps.echo "${jobName} doesn't contain / so we are out of guesses. Check you configuration and make sure that ${jobName} exists. Continue"
			return []
		}
	}
}

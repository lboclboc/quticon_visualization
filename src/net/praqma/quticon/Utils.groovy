package net.praqma.quticon

import org.jenkinsci.plugins.workflow.multibranch.WorkflowMultiBranchProject

static def getJobs(jobName) {

	def job = Jenkins.instance.getItem(jobName)
	echo "Hello!"
	if (job != null && job instanceof WorkflowMultiBranchProject) {
		println "${jobName} is a MultiBranchProject, return list of child jobs " + job.getAllJobs()
		return job.getAllJobs()
	}
	
	if (job != null) {
		return [job]
	}
	
	println "Job ${jobName} wasn't found. One possible option that it is a branch jobs of multibranch pipeline that can't be accessed usual way"
	println "Check if we are dealing with multibranch pipeline. First check for / in job the name"
	if (jobName.contains("/")) {
		def possibleMultiBranchParent = jobName.split("/")[0]
		println "/ found"
		println "Check if ${possibleMultiBranchParent} is a multibranch pipeline"
		def possibleMultiBranchParentJob = Jenkins.instance.getItem(possibleMultiBranchParent)
		if (possibleMultiBranchParentJob != null && possibleMultiBranchParentJob instanceof WorkflowMultiBranchProject) {
			println "It is a multibranch job. Extracted child job " + jobName.split("/")[1]
		        return [possibleMultiBranchParentJob.getItem(jobName.split("/")[1])]
		} else {
			println "${possibleMultiBranchParentJob} is either null or not multibranch pipeline. Skip ${jobName} and continue"
			return []
		}
	} else {
		println "${jobName} doesn't contain / so we are out of guesses. Check you configuration and make sure that ${jobName} exists. Continue"
		return []
	}
}

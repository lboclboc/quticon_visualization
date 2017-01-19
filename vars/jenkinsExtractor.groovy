import net.praqma.quticon.BuildDataEntry

// This method takes a list of job names and a time interval in hours
// and returns list of BuildDataEntry
// NonCPS added to avoid java.io.NotSerializableException's
// Read more here https://github.com/jenkinsci/workflow-cps-plugin#technical-design
@NonCPS
def call(def jobNames, def numberOfHoursBack) {
	def buildResults = []
    	for (def jobName: jobNames) {
		echo "Looking for the job with the name $jobName"
		def job = Jenkins.instance.getItem(jobName)
  		if (job == null) {
          		echo "Job {jobName} wasn't found. Check your configuration"
			continue
        	}
		def builds = job.getBuilds().byTimestamp(System.currentTimeMillis()-numberOfHoursBack*60*60*1000, System.currentTimeMillis()).completedOnly()
    		echo "Found ${builds.size()} builds matching time criteria for the job ${jobName}"
		for (def build: builds) {
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

import net.praqma.quticon.BuildDataEntry;

// This method takes a list of job names and a time interval in hours
// and returns list of BuildDataEntry
def call(def jobNames, def numberOfHoursBack) {
	def buildResults = []
    	for (def jobName: jobNames) {
		echo "Looking for the job with the name $jobName"	
		job = Jenkins.instance.getItem(jobName)
  		if (job == null) {
          		echo "Job {jobName} wasn't found. Check your configuration"
        	}
    		def builds = job.getBuilds().byTimestamp(System.currentTimeMillis()-numberOfHoursBack*60*60*1000, System.currentTimeMillis())
    		echo "Found ${builds.size()} builds matching time criteria for the job ${jobName}"
		for (def build: builds) {
			entry = new BuildDataEntry(jobName: jobName, verdict: build.result, duration: build.duration, timestamp: build.getTimestamp().getTimeInMillis())
			echo "Name ${entry.jobName}, result ${entry.verdict}, duration ${entry.duration}, timestamp ${entry.timestamp}"
            		buildResults.add(entry)
        	}
	}
	return buildResults
}

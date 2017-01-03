// This method takes a list of job names and a time interval in hours
def call(def jobNames, def numberOfHoursBack) {
	
	// Build list of jobs
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
        		def buildResult =[:]
        		buildResult["name"] = job.name
        		buildResult["result"] = build.result
            		buildResult["duration"] = build.duration
            		buildResult["timestamp"] = build.getTimestamp().getTimeInMillis()
			echo "Name ${job.name}, result ${build.result}, duration ${build.duration}, timestamp ${build.getTimestamp().getTimeInMillis()}"
            		buildResults.add(buildResult)
        	}
	}
	return buildResults
}

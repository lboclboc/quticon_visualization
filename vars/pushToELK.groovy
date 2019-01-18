// GrabResolver needed for old versions of Jenkins running Groovy 1.8 http://stackoverflow.com/questions/6335184/groovy-1-8-grab-fails-unless-i-manually-call-grape-resolve
@GrabResolver(name='http-builder', m2Compatible='true', root='http://repo1.maven.org/maven2/')
@Grapes(
   @Grab(group='org.codehaus.groovy.modules.http-builder', module='http-builder', version='0.7.1')
)

import groovyx.net.http.HTTPBuilder
import static groovyx.net.http.ContentType.*
import static groovyx.net.http.Method.*
import net.praqma.quticon.BuildDataEntry

// NonCPS added to avoid java.io.NotSerializableException's
// Read more here https://github.com/jenkinsci/workflow-cps-plugin#technical-design
@NonCPS
def call(def url, def index, def buildDataEntryList, def proxy_protocol=null, def proxy_host=null, def proxy_port=null) {
  mappings = """{ "mappings" : { "doc" : { "properties" : { 
                                                                  "verdict" : {"type": "string"},
                                                                  "job_name" : {"type": "string"},
                                                                  "build_number" : {"type": "integer"},
                                                                  "duration" : {"type": "integer"},
                                                                  "@timestamp" : {"type": "date"},
                                                                  "time_in_queue" : {"type": "integer"}
                                                                  "entry_type" : {"type": "string"}
  }}}}"""
  echo "Posting mappings to ${url}/${index}: ${mappings}"
  def http = new HTTPBuilder(url)
  if (proxy_host != null) {
     http.setProxy(proxy_host, proxy_port, proxy_protocol)
  }
  http.request( PUT, JSON ) { req ->
    body = mappings
    uri.path = "/${index}"
     
    response.success = { resp, json ->
        println "Success! ${resp.status}"
    }

    response.failure = { resp ->
        println "Request failed with status ${resp.status}"
    }
  }

  // Newer elastic does not support multiple types so only use doc here.
  def type = "doc"

  for (BuildDataEntry entry: buildDataEntryList)
  {
    def ts = new Date(entry.timestamp)
    def iso_date = ts.format("yyyy-MM-dd'T'HH:mm:ss'Z'", TimeZone.getTimeZone("UTC"))

    def data = """{
        "jobname": "${entry.job_name}",
        "verdict": "${entry.verdict}",
        "duration": ${entry.duration},
        "@timestamp": "${iso_date}",
        "time_in_queue": ${entry.time_in_queue},
        "build_number": ${entry.build_number},
        "entry_type": "${entry.entry_type}"
    }"""
    def index_date = ts.format("yyyy.MM.dd", TimeZone.getTimeZone("UTC"))
    def cleanJobName = entry.job_name.replace('/', '%2F')
    def cleanJobName = entry.job_name.replace(' ', '%20')
    def cleanJobName = entry.job_name.replace(':', '%3A')
    def uriPath ="${index}-${index_date}/${type}/${cleanJobName}:${entry.build_number}"
    echo "Post ${data} to ${url}/${uriPath}"
    http.post(path:uriPath, body:data, requestContentType:JSON) { resp, json ->
      echo json.toString()
      echo resp.statusLine.toString()
    }
  }
}

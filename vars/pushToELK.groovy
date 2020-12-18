// GrabResolver needed for old versions of Jenkins running Groovy 1.8 http://stackoverflow.com/questions/6335184/groovy-1-8-grab-fails-unless-i-manually-call-grape-resolve
@GrabResolver(name='http-builder', m2Compatible='true', root='http://repo1.maven.org/maven2/')
@Grapes(@Grab(group='org.codehaus.groovy.modules.http-builder', module='http-builder', version='0.7.1'))

import groovyx.net.http.HTTPBuilder
import static groovyx.net.http.ContentType.*
import static groovyx.net.http.Method.*
import net.praqma.quticon.BuildDataEntry
import groovy.json.StringEscapeUtils

// NonCPS added to avoid java.io.NotSerializableException's
// Read more here https://github.com/jenkinsci/workflow-cps-plugin#technical-design
@NonCPS
def call(def url, def index_base, def buildDataEntryList, def proxy_protocol=null, def proxy_host=null, def proxy_port=null)
{
    println("Publishing to elastic search on ${url}...")

    // Setup HTTP communication
    def http = new HTTPBuilder()

    if (proxy_host != null) {
        http.setProxy(proxy_host, proxy_port, proxy_protocol)
    }

    // Newer elastic does not support multiple types so only use doc here.
    def type = "doc"

    for (BuildDataEntry entry: buildDataEntryList)
    {
        def ts = new Date(entry.timestamp)
        def iso_date = ts.format("yyyy-MM-dd'T'HH:mm:ss'Z'", TimeZone.getTimeZone("UTC"))

        if (entry.description == null || entry.description == "null") {
            entry.description = ""
        }
        else {
            entry.description = StringEscapeUtils.escapeJava(entry.description)
        }

        def data = """{
            "jobname": "${entry.job_name}",
            "verdict": "${entry.verdict}",
            "duration": ${entry.duration},
            "@timestamp": "${iso_date}",
            "time_in_queue": ${entry.time_in_queue},
            "build_number": ${entry.build_number},
            "entry_type": "${entry.entry_type}",
            "description": "${entry.description}",
            "revisions": "${entry.revisions}"
        }"""
        def index_date = ts.format("yyyy.MM.dd", TimeZone.getTimeZone("UTC"))
        def cleanJobName = entry.job_name.replace('/', '%2F').replace(' ', '%20')
        def index_name = "${index_base}-${index_date}"

        uri = new URI("$url/${index_name}/${type}/${cleanJobName}%3A${entry.build_number}")
        println("Posting to ${uri}")
        try
        {
            http.post(uri:uri, body:data, requestContentType:JSON) { resp, json ->
                println(json.toString())
                println(resp.statusLine.toString())
            }
        }
        catch (Exception err) {
            println("Failed to post data: ${data}")
            println("Exception: ${err.message}")
        }
    }
    return 0
}

// vim: sw=4 ai et ts=4

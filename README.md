# Quticon
Quticon (QUality, TIme, CONtent) is a visualization tool for things like quality, time and content.

## Jenkins configuration

Configure Groovy Shared library to point to this repo - we have global pipeline steps defined inside directory vars

Then create Pipeline job and add the following flow definition where `["pipeline"]` is list with jobs names. So if you call your job `pipeline` and want to collect data for it then you need to have `["pipeline"]` as argument

```
def data = jenkinsExtractor(["pipeline"], 5)
pushToELK("http://188.166.73.120:9200", "jenkins", data)
```

Run pipeline job to get at least few builds to export or use already existing job.

## ELK setup

`188.166.73.120:9200` - in the pipeline configuration above is ip address and port of Elastic Search instance. You can get one for yourself by running docker-compose inside elk directory

```
cd elk
docker-compose up -d
```
Then open in the browser `docker container ip:5601` and create Jenkins index.

```
Index name or pattern: jenkins-*
Index contains time-based events: x
Time-field name: timestamp
```

Now you are ready to explore data. Click `Management\Saved Objects\Import` and import json file from elk/kibana_exports.
That will give you first basic searches that you can open at `Discover` and eventually dasboards and visualisations. (We just need to figure out how to configure them first)


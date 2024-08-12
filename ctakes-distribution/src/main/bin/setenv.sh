PRG="$0"
while [ -h "$PRG" ]; do
  ls=`ls -ld "$PRG"`
  link=`expr "$ls" : '.*-> \(.*\)$'`
  if expr "$link" : '/.*' > /dev/null; then
    PRG="$link"
  else
    PRG=`dirname "$PRG"`/"$link"
  fi
done
PRGDIR=`dirname "$PRG"`

# Only set CTAKES_HOME if not already set
[ -z "$CTAKES_HOME" ] && CTAKES_HOME=`cd "$PRGDIR/.." >/dev/null; pwd`

CLASS_PATH=$CTAKES_HOME/desc/:$CTAKES_HOME/resources/:$CTAKES_HOME/config/:$CTAKES_HOME/lib/*
#LOG4J2_PARM=-Dlog4j.configuration=file:$CTAKES_HOME/config/log4j2.xml
PIPE_RUNNER=org.apache.ctakes.core.pipeline.PiperFileRunner
PIPE_RUNNER_GUI=org.apache.ctakes.gui.pipeline.PiperRunnerGui
DICT_DOWNLOADER=org.apache.ctakes.gui.dictionary.DictionaryDownloader

FAST_PIPER=resources/org/apache/ctakes/clinical/pipeline/DefaultFastPipeline.piper

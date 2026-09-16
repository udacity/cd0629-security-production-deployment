#!/bin/bash
# Module 21: reads TaskFlow's structured log file, line by line, and
# indexes each line into Elasticsearch as its own document.
#
# In production, this collection step runs continuously and
# automatically, via an agent like Filebeat watching the file and
# shipping new lines the moment they're written. This script does the
# same job manually, once, so the recording can focus on the actual
# payoff — searching in Kibana — without also having to configure and
# debug a live-tailing agent on camera.

LOG_FILE="logs/taskflow.log"
ES_URL="http://localhost:9200/taskflow-logs/_doc"

if [ ! -f "$LOG_FILE" ]; then
  echo "No log file found at $LOG_FILE yet."
  echo "Trigger a few requests against TaskFlow first, then run this again."
  exit 1
fi

count=0
while IFS= read -r line; do
  if [ -n "$line" ]; then
    curl -s -o /dev/null -X POST "$ES_URL" \
      -H "Content-Type: application/json" \
      -d "$line"
    count=$((count + 1))
  fi
done < "$LOG_FILE"

echo "Indexed $count log line(s) into Elasticsearch, index: taskflow-logs"

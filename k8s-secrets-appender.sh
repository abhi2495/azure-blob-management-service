##########
# This shell script appends secrets data to the end of values.yaml of the chart of this microservice
# To call the shell script: sh k8s-secrets-appender.sh 'secretName1=secretVal1 secretName2=secretVal2'
##########

MAP_DATA=$1

YAMLSTR="\n  secrets:\n    enabled: true\n    data:"

IFS=' '
read -a MAP_ENTRIES <<<"$MAP_DATA"



for MAP_ENTRY in "${MAP_ENTRIES[@]}"; do
  KEY="$( cut -d '=' -f 1 <<< "$MAP_ENTRY" )";
  VAL="$( cut -d '=' -f 2- <<< "$MAP_ENTRY" )";
  ENCODED_VAL=$(echo $VAL | base64)
  YAMLSTR="$YAMLSTR\n      $KEY: $ENCODED_VAL"
done

echo "$YAMLSTR" > secrets-data.yaml

cat secrets-data.yaml >> charts/azure-blob-management-service/values.yaml

rm -rf secrets-data.yaml
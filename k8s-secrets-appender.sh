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
  ENCODED_VAL=$(echo -n $VAL | base64 -w 0)
# Note : echo -n removes trailing newline which is added by default
# Note : base64 by default wraps if the encoded length is more than 76. To disable wrapping we are using -w 0
  YAMLSTR="$YAMLSTR\n      $KEY: $ENCODED_VAL"
done

echo -e "$YAMLSTR" > secrets-data.yaml

cat secrets-data.yaml >> charts/azure-blob-management-service/values.yaml

rm -rf secrets-data.yaml
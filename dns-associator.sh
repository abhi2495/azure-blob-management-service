DNSNAME=$1
for i in $(seq 1 15); do
  EXTERNAL_IP=`kubectl get service azure-blob-management-service-nginx-ingress-controller | grep -v EXTERNAL | awk '{print $4}'`
  if [ "$EXTERNAL_IP" = "<pending>" ]; then
    echo "Waiting for a minute for external IP to get created"
    sleep 60
  else
    break
  fi
done

if [ "$EXTERNAL_IP" = "<pending>" ]; then
  echo "Timeout waiting for external IP to get created"
  exit 1
fi

PUBLIC_IP_ID=`az network public-ip list --query "[?ipAddress!=null]|[?contains(ipAddress, '$EXTERNAL_IP')].[id]" --output tsv`

az network public-ip update --ids $PUBLIC_IP_ID --dns-name $DNSNAME
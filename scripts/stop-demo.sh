group=$1

if [ "$group" == "infra" ]; then
  kubectl delete -f kconfig/infra/
elif [ "$group" == "app" ]; then
  kubectl delete -f kconfig/app/
elif [ "$group" == "db" ]; then
  kubectl delete -f kconfig/infra/35-sql-edge-kube.yaml -f kconfig/infra/38-sql-edge-init-job.yaml
elif [ "$group" == "wiremock" ]; then
  kubectl delete -f kconfig/wiremock/
else
  kubectl delete -f kconfig/app/ && kubectl delete -f kconfig/infra/
fi
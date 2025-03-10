group=$1

if [ "$group" == "infra" ]; then
  kubectl delete -f kconfig/infra/
elif [ "$group" == "app" ]; then
  kubectl delete -f kconfig/app/
elif [ "$group" == "cron" ]; then
  kubectl delete -f kconfig/cron/50-spring-demo-kube-cronjob.yaml
elif [ "$group" == "db" ]; then
  kubectl delete -f kconfig/infra/35-sql-edge-kube.yaml -f kconfig/infra/38-sql-edge-init-job.yaml
elif [ "$group" == "db-init" ]; then
  kubectl delete -f kconfig/infra/38-sql-edge-init-job.yaml
elif [ "$group" == "wiremock" ]; then
  kubectl delete -f kconfig/wiremock/
elif [ "$group" == "ingress-nginx" ]; then
  kubectl delete -f https://raw.githubusercontent.com/kubernetes/ingress-nginx/controller-v1.6.4/deploy/static/provider/cloud/deploy.yaml
elif [ "$group" == "ingress" ]; then
  kubectl delete -f kconfig/ingress/
elif [ "$group" == "all" ]; then
  kubectl delete -f kconfig/wiremock/
  kubectl delete -f kconfig/ingress/
  kubectl delete -f kconfig/app/
  kubectl delete -f https://raw.githubusercontent.com/kubernetes/ingress-nginx/controller-v1.6.4/deploy/static/provider/cloud/deploy.yaml
  kubectl delete -f kconfig/infra/
else
  kubectl delete -f kconfig/app/ && kubectl delete -f kconfig/infra/
fi
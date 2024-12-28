group=$1

if [ "$group" == "infra" ]; then
  kubectl apply -f kconfig/infra/ && kubectl -n spring-demo get pods --watch
elif [ "$group" == "app" ]; then
  kubectl apply -f kconfig/app/ && kubectl -n spring-demo get pods --watch
elif [ "$group" == "cron" ]; then
  kubectl apply -f kconfig/app/50-spring-demo-kube-cronjob.yaml && kubectl -n spring-demo get cronjob --watch
elif [ "$group" == "db" ]; then
  kubectl apply -f kconfig/infra/35-sql-edge-kube.yaml -f kconfig/infra/38-sql-edge-init-job.yaml && kubectl -n spring-demo get pods --watch
elif [ "$group" == "db-init" ]; then
  kubectl apply -f kconfig/infra/38-sql-edge-init-job.yaml && kubectl -n spring-demo get pods --watch
elif [ "$group" == "wiremock" ]; then
  kubectl apply -f kconfig/wiremock/ && kubectl -n wiremock-demo get pods --watch
else
  kubectl apply -f kconfig/infra/ && sleep 30 && kubectl apply -f kconfig/app/ && kubectl -n spring-demo get pods --watch
fi

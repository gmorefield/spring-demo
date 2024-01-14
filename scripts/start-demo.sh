group=$1

if [ "$group" == "infra" ]; then
  kubectl apply -f kconfig/infra/ && kubectl -n spring-demo get pods --watch
elif [ "$group" == "app" ]; then
  kubectl apply -f kconfig/app/ && kubectl -n spring-demo get pods --watch
elif [ "$group" == "db" ]; then
  kubectl apply -f kconfig/infra/35-sql-edge-kube.yaml -f kconfig/infra/38-sql-edge-init-job.yaml && kubectl -n spring-demo get pods --watch
else
  kubectl apply -f kconfig/infra/ && sleep 30 && kubectl apply -f kconfig/app/ && kubectl -n spring-demo get pods --watch
fi

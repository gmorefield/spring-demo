group=$1

if [ "$group" == "infra" ]; then
  kubectl apply -f kconfig/infra/ && kubectl -n spring-demo get pods --watch
elif [ "$group" == "app" ]; then
  kubectl apply -f kconfig/app/ && kubectl -n spring-demo get pods --watch
else
  kubectl apply -f kconfig/infra/ && sleep 30 && kubectl apply -f kconfig/app/ && kubectl -n spring-demo get pods --watch
fi

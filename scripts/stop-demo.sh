group=$1

if [ "$group" == "infra" ]; then
  kubectl delete -f kconfig/infra/
elif [ "$group" == "app" ]; then
  kubectl delete -f kconfig/app/
else
  kubectl delete -f kconfig/app/ && kubectl delete -f kconfig/infra/
fi
group=$1

if [ "$group" == "db" ]; then
  kubectl -n spring-demo logs -f -l app=sqledge
elif [ "$group" == "db-init" ]; then
  kubectl -n spring-demo logs -f job.batch/sqledge-init-job
else
  kubectl -n spring-demo logs -f -l app=spring-demo-app
fi
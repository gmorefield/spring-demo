group=$1

if [ "$group" == "db" ]; then
  kubectl -n spring-demo logs -f -l app=sqledge
elif [ "$group" == "db-init" ]; then
  kubectl -n spring-demo logs -f job.batch/sqledge-init-job
elif [ "$group" == "ingress" ]; then
  kubectl -n ingress-nginx logs -f job.batch/sqledge-init-job
elif [ "$group" == "wiremock" ]; then
  kubectl -n wiremock-demo logs -f -l app=wiremock
else
  kubectl -n spring-demo logs -f -l app=spring-demo-app
fi
option=$1

if [ "$option" == "restart" ]; then
  ./mvnw package && docker build -t spring-demo:latest . && kubectl -n spring-demo rollout restart deployment.apps/spring-demo-app && kubectl -n spring-demo get pods --watch
elif [ "$option" == "deploy" ]; then
  ./mvnw package && docker build -t spring-demo:latest . && start-demo.sh app
elif [ "$option" == "restart-skiptests" ]; then
  ./mvnw -Dmaven.test.skip=true package && docker build -t spring-demo:latest . && kubectl -n spring-demo rollout restart deployment.apps/spring-demo-app && kubectl -n spring-demo get pods --watch
else
  ./mvnw package && docker build -t spring-demo:latest .
fi
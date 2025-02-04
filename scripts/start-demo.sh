group=$1

if [ "$group" == "infra" ]; then
  kubectl apply -f kconfig/infra/ && kubectl -n spring-demo get pods --watch
elif [ "$group" == "app" ]; then
  kubectl apply -f kconfig/app/ && kubectl -n spring-demo get pods --watch
elif [ "$group" == "cron" ]; then
  kubectl apply -f kconfig/cron/50-spring-demo-kube-cronjob.yaml && kubectl -n spring-demo get cronjob --watch
elif [ "$group" == "db" ]; then
  kubectl apply -f kconfig/infra/35-sql-edge-kube.yaml -f kconfig/infra/38-sql-edge-init-job.yaml && kubectl -n spring-demo get pods --watch
elif [ "$group" == "db-init" ]; then
  kubectl apply -f kconfig/infra/38-sql-edge-init-job.yaml && kubectl -n spring-demo get pods --watch
elif [ "$group" == "wiremock" ]; then
  kubectl apply -f kconfig/wiremock/ && kubectl -n wiremock-demo get pods --watch
elif [ "$group" == "ingress-nginx" ]; then
  kubectl apply -f https://raw.githubusercontent.com/kubernetes/ingress-nginx/controller-v1.6.4/deploy/static/provider/cloud/deploy.yaml
elif [ "$group" == "helm-ingress-nginx" ]; then
  helm upgrade --install ingress-nginx ingress-nginx \\
    --repo https://kubernetes.github.io/ingress-nginx \\
    --namespace ingress-nginx --create-namespacekubectl apply -f https://raw.githubusercontent.com/kubernetes/ingress-nginx/master/deploy/mandatory.yaml
  kubectl apply -f https://raw.githubusercontent.com/kubernetes/ingress-nginx/master/deploy/provider/cloud-generic.yaml
elif [ "$group" == "ingress" ]; then
  kubectl apply -f kconfig/ingress/ && kubectl -n ingress-nginx get svc
else
  kubectl apply -f kconfig/infra/ && sleep 30 && kubectl apply -f kconfig/app/ && kubectl -n spring-demo get pods --watch
fi

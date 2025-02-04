alias k=kubectl
alias ksd="kubectl -n spring-demo"
alias kin="kubectl -n ingress-nginx"

export PATH="${PWD}/scripts:$PATH"

echo "Enter db password"
read LB_PASS

export LB_USERNAME=sa
export LB_PASSWORD=${LB_PASS}
export LIQUIBASE_COMMAND_USERNAME=${LB_USERNAME}
export LIQUIBASE_COMMAND_PASSWORD=${LB_PASSWORD}

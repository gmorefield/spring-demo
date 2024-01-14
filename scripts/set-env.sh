alias k=kubectl
alias ksd="kubectl -n spring-demo"

export PATH="${PWD}/scripts:$PATH"

echo "Enter db password"
read LB_PASS

export LB_USERNAME=sa
export LB_PASSWORD=${LB_PASS}

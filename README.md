# Spring Demo

Sample application to demonstrate some of the features of Spring Boot

Several Spring profiles have been added to demonstrate different behavior

### Profiles to control database configuration:
| Profile | Description |
| ----- | ------ |
| default | uses ```spring.datasource``` and ```spring.datasource.hikari``` properties |
| builder | hard codes database configuration |
| props | uses ```ConfigurationProperties``` with ```spring.datasource``` properties |
| h2 | similar to default, but pulls ```spring.datasource properties``` from ```application-h2.yml``` |
| mssql | used to connect to local SQL Server instance |

### Profiles to control security:

By default, the application exposes a user with name `sample-client` (in `application.properties`)

| Profile | Description |
| ----- | ------ |
| default | anonymous access to all calls |
| basic | simple basic auth example. pull password from application logs or define ```spring.security.user.password``` property |

### Profiles to control actuators:
| Profile | Description |
| ----- | ------ |
| default | enables health and info actuators |
| local | enables all actuators |


## Ways to run application:

### 1) Maven spring-boot:run task
Simplest case, ```./mvnw spring-boot:run```

Can specify override parameters and profiles, for example to set default user password enable h2 profile
```` sh
./mvnw spring-boot:run -Dspring-boot.run.arguments="--spring.security.user.password=changeit" -Dspring-boot.run.profiles=h2
````

### 2) Java Jar
Make the project first and then run the uber jar

Simplest case:
```sh
./mnvw package
java -jar target/*.jar
```

#### using system.properties to control properties and profiles
```` sh
./mvnw package
java -Dspring.profiles.active=local -jar target/*.jar
````

#### using command-line parameters to control properties and profiles
```` sh
./mvnw package
java -jar target/*.jar --spring.security.user.password=changeit --spring.profiles.active=basic
````

#### using env vars to control properties and profiles
```` sh
./mvnw package
SPRING_PROFILES_ACTIVE=basic SPRING_SECURITY_USER_PASSWORD=changeit java -jar target/*.jar
````

## Commands to Run

```sh
# add a person
curl -X POST http://localhost:8080/person -H "Content-Type: application/json" -d "{\"firstName\":\"john\",\"lastName\":\"doe\"}"

# find all persons
curl http://localhost:8080/person

# health
curl http://localhost:8080/actuator/health

# info
curl http://localhost:8080/actuator/info
```

Notes:
  - If the `basic` profile is enabled, add `-u sample-client` to the curl commands for /person urls and enter the password when prompted. The user is also needed for any actuator other than info and health 
  - On Windows, you may need to add `winpty` in front of `curl` command, eg. ```winpty curl http://localhost:8080/actuator/info```

## Change Log

| Version        | Description                                                                      |
|----------------|----------------------------------------------------------------------------------|
| 0.0.1-SNAPSHOT | initial version                                                                  |
| 0.0.2          | minor config cleanup; added Spring Cache based on ```sample.cache.enabled=true``` |
| 0.1.0          | merged lots of spring cloud changes                                              |
| 0.2.0          | upgrade to latest versions (JDK 17, Spring Boot 3.2 / Spring 6.1.2               |
| 0.3.0          | add liquibase support and reorg k8s configs/scripts                              |

## Docker setup for misc profile features
### springdemo
```sh
docker build -t spring-demo:0.0.1 .
docker run --rm --name spring-demo -p 30005:8080 -e 'SPRING_PROFILES_ACTIVE=h2,local' --detach spring-demo:0.0.1
```

### mssql
```sh
docker run --cap-add SYS_PTRACE -e 'ACCEPT_EULA=1' -e 'MSSQL_SA_PASSWORD={password}' -p 1433:1433 --rm --name sqledge --detach mcr.microsoft.com/azure-sql-edge

docker run --rm -ti --platform linux/amd64 mcr.microsoft.com/mssql-tools:latest
/opt/mssql-tools/bin/sqlcmd -S sqledge -U sa -C -Q "create database TEST" 
```

### activemq: 
```sh
docker run --name activemq --rm --detach -p 61616:61616 -p 8161:8161 symptoma/activemq:latest
```

### mq: (IBM MQ)
- Docker Container: [Get an IBM MQ queue for development in a container](https://developer.ibm.com/tutorials/mq-connect-app-queue-manager-containers/)
- Administration: [Web Console](https://{ip}:9443/ibmmq/console/)
  - [Getting started with the web console](https://www.ibm.com/docs/en/ibm-mq/9.1?topic=console-getting-started-web)
```sh
sudo docker run --rm --name QM1 --env LICENSE=accept --env MQ_QMGR_NAME=QM1 --publish 1414:1414 --publish 9443:9443 --detach icr.io/ibm-messaging/mq:latest
```
Note: UTM required on Mac silicon
```sh
ip addr show
```

## K8s setup
### springdemo
```sh
k apply -f kconfig/
k -n spring-demo get all
k -n spring-demo describe deployment spring-demo-app
k -n spring-demo rollout restart deployment spring-demo-app
k -n spring-demo logs -f -l app=spring-demo-app
k -n spring-demo delete jobs --field-selector status.successful=1
k -n spring-demo exec spring-demo-app-xxxx-xx -it -- /bin/sh

k -n spring-demo run -i --tty --attach sqlcmd --image=mcr.microsoft.com/mssql-tools:latest
ksd attach sqlcmd -c sqlcmd -i -t
```

### Liquibase
References
* [Properties](https://docs.liquibase.com/concepts/connections/creating-config-properties.html)
  * Can reference liquibase properties with `liquibase.` prefix
  * e.g `<property name="schema-name" value="${liquibase.liquibaseSchemaName}"/>`
* [Parameters](https://docs.liquibase.com/parameters/home.html)
  * prefix with parameter in properties file
  * e.g. `parameter.my_parameter = something`
* [Tag](https://docs.liquibase.com/commands/utility/tag.html)
  * useful for setting rollback point
* [Preconditions](https://docs.liquibase.com/concepts/changelogs/preconditions.html)
* [Contexts](https://docs.liquibase.com/concepts/changelogs/attributes/contexts.html)
  * useful for denoting tests or other scenarios
  * the order of operations is !, AND, and then OR (comma is same as or)
    * eg. `test, qa and main` is the same as `(test) OR (qa and main)`

To update runtime
```shell
./mvnw compile liquibase:update
```
To load test schema
```shell
./mvnw compile liquibase:update -Dlb-property-file=/db/changelog/liquibase.properties -Dschema-name=Test
```
To load runtime into alternate schema
```shell
# see changes to apply (output in target/liquibase/migrate.sql)
./mvnw compile liquibase:updateSQL -Dlb-property-file=/liquibase/liquibase.properties -Dliquibase.liquibaseSchemaName=trial
# apply changes
./mvnw compile liquibase:update -Dlb-property-file=/liquibase/liquibase.properties -Dschema-name=trial -Dliquibase.liquibaseSchemaName=trial -Dliquibase.schemas=trial
```
To tag current version
```shell
./mvnw compile liquibase:tag -Dlb-property-file=/liquibase/liquibase.properties -Dliquibase.liquibaseSchemaName=trial -Dliquibase.tag=tag1
```
To rollback
```shell
# to see rollback changes
./mvnw compile liquibase:rollbackSQL -Dlb-property-file=/liquibase/liquibase.properties -Dliquibase.liquibaseSchemaName=trial -Dliquibase.rollbackTag=tag1
# to apply rollback (also supports changesets and date)
./mvnw compile liquibase:rollback -Dlb-property-file=/liquibase/liquibase.properties -Dliquibase.liquibaseSchemaName=trial -Dliquibase.rollbackTag=tag1
```
To dropAll
```shell
# to drop all tables
./mvnw compile liquibase:dropAll -Dlb-property-file=/liquibase/liquibase.properties -Dliquibase.liquibaseSchemaName=Test -Dliquibase.schemas=Test
```
### compile, build, deploy
```sh
alias ksd="kubectl -n spring-demo"
./mvnw package && docker build -t spring-demo:latest . && ksd rollout restart deployment spring-demo-app && ksd get pods --watch
```

### endpoints
[API Reference](https://kubernetes.io/docs/reference/kubernetes-api/service-resources/endpoints-v1/)
```sh
kubectl auth can-i list endpoints --as=system:serviceaccount:spring-demo:default -n spring-demo

wget -O - --no-check-certificate \
  --header="Authorization: Bearer $(cat /var/run/secrets/kubernetes.io/serviceaccount/token)" \
  --header="Accept: application/json;as=Table;g=meta.k8s.io;v=v1" \ 
  https://kubernetes.default.svc.cluster.local/api/v1/namespaces/spring-demo/endpoints/spring-demo-app
```

## HashiCorp Vault
References
- [Injecting Secrets](https://developer.hashicorp.com/vault/tutorials/kubernetes/kubernetes-sidecar)
### Development mode setup (helm)
```sh
helm repo add hashicorp https://helm.releases.hashicorp.com
helm repo update
helm install vault hashicorp/vault --set "server.dev.enabled=true"
```
### Vault configuration
```sh
kubectl exec -it vault-0 -- /bin/sh
vault secrets enable -path=secret kv-v2
vault kv put secret/spring-demo username="xxx" password="xxx"
```


## Misc Notes
```sh
./mvnw versions:display-dependency-updates
./mvnw versions:display-plugin-updates
```

### Version updates
[Increment versions in maven builds](https://wyssmann.com/blog/2021/03/how-to-increment-versions-in-maven-builds-alternative-to-maven-release-plugin/)
```sh
./mvnw build-helper:parse-version help:effective-pom

./mvnw build-helper:parse-version versions:set \
 -DnewVersion=\${parsedVersion.majorVersion}.\${parsedVersion.minorVersion}.\${parsedVersion.incrementalVersion}
 
./mvnw build-helper:parse-version versions:set \
-DnewVersion=\${parsedVersion.majorVersion}.\${parsedVersion.minorVersion}.\${parsedVersion.nextIncrementalVersion}-SNAPSHOT

```

### PlantUML download
1. Download [PlantUMLrelease jar](https://github.com/plantuml/plantuml/releases) from git 

2. Install downloaded jar into Maven repository:
```sh
./mvnw install:install-file -DlocalRepositoryPath=/Users/morefigs/.m2/repository -DcreateChecksum=true -Dpackaging=jar -Dfile=/Users/morefigs/Downloads/plantuml-1.2023.6.jar -DgroupId=plantuml -DartifactId=plantuml -Dversion=v1.2023.6
```

### Quartz
* References
  * [Project Home](https://www.quartz-scheduler.org/)
  * [Full Docs](https://github.com/quartz-scheduler/quartz/blob/main/docs/index.adoc)
  * [How to Setup Databases](https://github.com/quartz-scheduler/quartz/wiki/How-to-Setup-Databases#using-liquibase-tool)
  * Spring Integration
    * [Quartz Scheduler](https://docs.spring.io/spring-boot/reference/io/quartz.html)
  * [Configuration Reference](https://www.quartz-scheduler.org/documentation/quartz-2.3.0/configuration/ConfigMain.html)

### Review possibilities
* Kustomize
  * [docs](https://kubectl.docs.kubernetes.io/references/kustomize/)
  * [images transformer](https://kubectl.docs.kubernetes.io/references/kustomize/kustomization/images/)
    * [dynamic images](https://stackoverflow.com/questions/66317628/how-to-use-dynamic-variable-image-tag-in-a-kubernetes-deployment)
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

| Version | Description |
| ----- | ------ |
| 0.0.1-SNAPSHOT | initial version |
| 0.0.2 | minor config cleanup; added Spring Cache based on ```sample.cache.enabled=true``` |
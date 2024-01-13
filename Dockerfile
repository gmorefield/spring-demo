# FROM adoptopenjdk/openjdk11
FROM bellsoft/liberica-openjdk-alpine-musl:17

# RUN apk add --no-cache msttcorefonts-installer fontconfig
# RUN update-ms-fonts

RUN apk --no-cache add msttcorefonts-installer fontconfig && \
    update-ms-fonts && \
    fc-cache -f

RUN addgroup --system spring && adduser --system spring --ingroup spring
USER spring:spring
ARG JAR_FILE=target/*.jar
COPY ${JAR_FILE} app.jar

ENV JAVA_TOOL_OPTIONS="-XX:MinRAMPercentage=50 -XX:MaxRAMPercentage=80"
ENTRYPOINT ["java", "-jar", "/app.jar"]
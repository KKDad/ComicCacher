FROM eclipse-temurin:21-jdk
LABEL MAINTAINER="adrian@gilbert.ca"
VOLUME /tmp

EXPOSE 8888

ARG VERSION
COPY build/libs/comic-api-${VERSION}.jar /app.jar

ENTRYPOINT [ "java", "-jar", "/app.jar" ]

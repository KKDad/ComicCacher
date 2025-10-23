FROM openjdk:21-jdk
LABEL MAINTAINER="adrian@gilbert.ca"
VOLUME /tmp

EXPOSE 8888

COPY build/libs/comic-api-1.2.0-SNAPSHOT.jar /

ENTRYPOINT [ "java", "-jar", "/comic-api-1.2.0-SNAPSHOT.jar" ]

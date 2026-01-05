FROM eclipse-temurin:21-jdk
LABEL MAINTAINER="adrian@gilbert.ca"
VOLUME /tmp

EXPOSE 8888

COPY build/libs/comic-api-2.3.1.jar /

ENTRYPOINT [ "java", "-jar", "/comic-api-2.3.1.jar" ]

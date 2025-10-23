FROM openjdk:21-jdk
LABEL MAINTAINER="adrian@gilbert.ca"
VOLUME /tmp

EXPOSE 8888

COPY build/libs/comic-api-2.1.36.jar /

ENTRYPOINT [ "java", "-jar", "/comic-api-2.1.36.jar" ]

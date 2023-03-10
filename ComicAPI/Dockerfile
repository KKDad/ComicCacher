FROM eclipse-temurin:17-jre-alpine
LABEL MAINTAINER="adrian@gilbert.ca"
VOLUME /tmp

EXPOSE 8888

COPY build/libs/ComicAPI-1.0.0-SNAPSHOT.jar /

ENTRYPOINT [ "java", "-jar", "/ComicAPI-1.2.0-SNAPSHOT.jar" ]

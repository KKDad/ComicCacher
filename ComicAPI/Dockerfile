FROM eclipse-temurin:11.0.16.1_1-jre
LABEL MAINTAINER="adrian@gilbert.ca"
VOLUME /tmp

EXPOSE 8888

COPY build/libs/ComicAPI-1.0.0-SNAPSHOT.jar /

ENTRYPOINT [ "java", "-jar", "/ComicAPI-1.0.0-SNAPSHOT.jar" ]

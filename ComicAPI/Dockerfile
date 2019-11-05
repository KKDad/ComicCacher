FROM openjdk:8-jdk-alpine
LABEL MAINTAINER="adrian@gilbert.ca"
VOLUME /tmp
ARG DEPENDENCY=target/dependency

EXPOSE 8888

ENTRYPOINT ["java", "-cp", "app:app/lib/*", "org.stapledon.api.ComicApiApplication"]

COPY ${DEPENDENCY}/BOOT-INF/lib /app/lib
COPY ${DEPENDENCY}/META-INF /app/META-INF
COPY ${DEPENDENCY}/BOOT-INF/classes /app
COPY ${DEPENDENCY}/static /app/static
FROM openjdk:8-jdk-alpine
MAINTAINER adrian@gilbert.ca
VOLUME /tmp
ARG DEPENDENCY=target/dependency

COPY ${DEPENDENCY}/BOOT-INF/lib /app/lib
COPY ${DEPENDENCY}/META-INF /app/META-INF
COPY ${DEPENDENCY}/BOOT-INF/classes /app

EXPOSE 8888

ENTRYPOINT ["java", "-cp", "app:app/lib/*", "com.stapledon.comic.ComicApiApplication"]
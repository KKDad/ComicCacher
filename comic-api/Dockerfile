FROM docker.io/library/eclipse-temurin:21.0.12_8-jre-alpine-3.24
LABEL MAINTAINER="adrian@gilbert.ca"

RUN apk add --no-cache wget && \
    addgroup --system --gid 1001 comicapi && \
    adduser --system --uid 1001 comicapi

EXPOSE 8888

ARG VERSION
COPY --chown=comicapi:comicapi build/libs/comic-api-${VERSION}.jar /app.jar

USER comicapi

HEALTHCHECK --interval=30s --timeout=3s CMD wget -q --spider http://localhost:8888/actuator/health || exit 1

ENTRYPOINT [ "java", "-jar", "/app.jar" ]

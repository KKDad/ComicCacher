FROM eclipse-temurin:21-jre-alpine
LABEL MAINTAINER="adrian@gilbert.ca"

RUN addgroup --system --gid 1001 comicapi && \
    adduser --system --uid 1001 comicapi

EXPOSE 8888

ARG VERSION
COPY --chown=comicapi:comicapi build/libs/comic-api-${VERSION}.jar /app.jar

USER comicapi

HEALTHCHECK --interval=30s --timeout=3s CMD wget -q --spider http://localhost:8888/actuator/health || exit 1

ENTRYPOINT [ "java", "-jar", "/app.jar" ]

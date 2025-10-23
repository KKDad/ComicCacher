# -- Stage 1
FROM node:20 AS build-step

RUN mkdir -p /app
WORKDIR /app
COPY package.json /app
RUN npm install
COPY . /app
RUN npm run build --omit=dev

# -- Stage 2
FROM eclipse-temurin:11-jre-alpine
COPY --from=build-step /app/dist/comic-web /static_content

ENV VERSION=9.4.49.v20220914
RUN apk add --update bash wget tar && rm -rf /var/cache/apk/*
RUN wget -q -O /jetty.tar.gz https://repo1.maven.org/maven2/org/eclipse/jetty/jetty-distribution/${VERSION}/jetty-distribution-$VERSION.tar.gz

# Install Jetty
RUN tar -xvf /jetty.tar.gz && rm /jetty.tar.gz && mv jetty-distribution-${VERSION} /jetty
COPY entrypoint.sh static_content.xml /

RUN chmod +x entrypoint.sh && apk del wget tar

EXPOSE 8080

ENTRYPOINT /entrypoint.sh

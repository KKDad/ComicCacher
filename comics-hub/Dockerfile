# -- Stage 1: Build Next.js
FROM node:22-alpine AS build-step

WORKDIR /app
COPY package.json package-lock.json ./
RUN npm ci
COPY . .

# Build-time env: backend is reached via Docker network hostname
ARG NEXT_PUBLIC_GRAPHQL_ENDPOINT=http://comics-api:8888/graphql
ENV NEXT_PUBLIC_GRAPHQL_ENDPOINT=$NEXT_PUBLIC_GRAPHQL_ENDPOINT

RUN npm run build

# -- Stage 2: Run with Node
FROM node:22-alpine

WORKDIR /app

# Copy the standalone output and static assets
COPY --from=build-step /app/.next/standalone ./
COPY --from=build-step /app/.next/static ./.next/static
COPY --from=build-step /app/public ./public

ENV NODE_ENV=production
ENV PORT=8080

EXPOSE 8080

CMD ["node", "server.js"]

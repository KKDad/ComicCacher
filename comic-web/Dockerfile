# -- Stage 1: Build Angular
FROM node:22 AS build-step

RUN mkdir -p /app
WORKDIR /app
COPY package.json package-lock.json /app/
RUN npm ci
COPY . /app
RUN npm run build --omit=dev

# -- Stage 2: Serve with Nginx
FROM nginx:alpine

# Copy Angular build output
COPY --from=build-step /app/dist/comic-web/browser /usr/share/nginx/html

# Copy nginx configuration
COPY nginx.conf /etc/nginx/conf.d/default.conf

EXPOSE 8080

CMD ["nginx", "-g", "daemon off;"]

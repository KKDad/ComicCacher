FROM node:16 as build-step

RUN mkdir -p /app
WORKDIR /app

COPY package.json /app
RUN npm install

COPY . /app
RUN npm run build --prod

FROM nginx:1.20.1
COPY --from=build-step /app/dist/ComicViewer /usr/share/nginx/html
COPY nginx.conf /etc/nginx/nginx.conf

EXPOSE 4200:80


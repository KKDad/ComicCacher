# The Comic Processor

Web Comic downloader v2.0. A rethink of the initial comic caching program.

#### Caching Comics

#### Comic API

To build and launch the Comic API docker container and share comics mounted from a remote share:
~~~
 gradlew :ComicAPI:docker
 mkdir /backups
 mount 192.168.1.7:/volume1/Backups /backups
 docker run -it -v /backups/ComicCache:/data -p 8080:8888 kkdad/comic-api
~~~

To export the docker file for transferring between machines without using docker hub:
~~~
docker save kkdad/comic-api -o comics-api.docker

docker load -i comics-api.docker
docker create --name comics -v /backups/ComicCache:/data -p 8080:8888 --restart unless-stopped kkdad/comic-api:latest
dockler start comics
~~~

#### ComicViewer

Create a class/interface from json to typescript by using this site: http://json2ts.com/

To run:
~~~
npm install
ng serve
~~~

Material Design Principles
https://material.io/design/components/cards.html#usage

Material Design in Angular
https://material.angular.io/components/categories

Building Infinite Virtual Scrolling Lists with the new Angular 7 Cdk
https://pusher.com/tutorials/infinite-scrolling-angular-cdk
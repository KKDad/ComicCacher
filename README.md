# The Comic Processor

Web Comic downloader v2.0. A rethink of the initial comic caching program.

#### Caching Comics

#### Comic API

To build and launch the Comic API, build the docker container, tag and push the image, then run a helm upgrade
~~~
gradlew :ComicAPI:docker
docker images 
docker tag kkdad/comic-api:latest registry.local613.local:5000/kkdad/comic-api:2.0.1
docker push registry.local613.local:5000/kkdad/comic-api:2.0.1

helm upgrade comics comics
~~~

To view the API:
- https://comics.gilbert.ca/docs/index.html
- https://comics.gilbert.ca/api/v1/comics

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
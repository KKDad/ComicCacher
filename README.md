# The Comic Processor

Webcomic scroller v2.0. Originally written back in 2013 in C# and .Net 3.0, this has since been re-imagined and rebuilt using modern stack:
- Spring Boot 3.0, Java 17, Dockerized backend
    - Read and cache comics with cleanup after 7 days
    - Expose REST API
    - OpenAPI 3.0 documentation
- Angular + Material Design, Dockerized Frontend
- Hosted in a K8s environment

There's no public-facing deployment of this service - I developed it for my own usage and for fun. If you'd like to use
it yourself, go ahead. 
 
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
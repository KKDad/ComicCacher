# The Comic Processor

Web Comic downloader v2.0. A rethink of the initial comic caching program.

#### Caching Comics

#### Comic API

To build and launch the Comic API docker container
~~~
 gradlew :ComicAPI:docker
 docker run --publish=8888:8888 kkdad/comic-api --name comic-api
~~~

#### ComicViewer

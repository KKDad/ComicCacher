# The Comic Processor

Webcomic scroller v2.0. Originally written back in 2013 in C# and .Net 3.0, this has since been re-imagined and rebuilt using modern stack:
- Spring Boot 3.4.5, Java 21, Dockerized backend
    - Read and cache comics with cleanup after 7 days
    - Expose REST API with standardized response format
    - OpenAPI 3.0 documentation with Swagger UI
- Angular + Material Design, Dockerized Frontend
- Hosted in a K8s environment

There's no public-facing deployment of this service - I developed it for my own usage and for fun. If you'd like to use
it yourself, go ahead.

## Features

### Caching Comics
- Daily comic strip download from GoComics and ComicsKingdom
- Thread-safe image caching with automatic cleanup
- Optimized resource management and error handling
- Configurable cache location and retention policy

### API Features
- RESTful endpoints for comic retrieval and management
- Standardized API responses with consistent formatting
- Proper exception handling with informative error messages
- Optional filtering by comic name, date, and other parameters

## Building and Running

### Comic API

#### Local Development
```bash
# Build the project
./gradlew :ComicAPI:build

# Run tests
./gradlew :ComicAPI:test

# Run integration tests
./gradlew :ComicAPI:integrationTest

# Run the application locally
./gradlew :ComicAPI:bootRun
```

#### Docker Deployment
To build and launch the Comic API, build the docker container, tag and push the image, then run a helm upgrade
```bash
./gradlew :ComicAPI:build
./ComicAPI/build-docker.sh 2.1.0
docker images
docker tag kkdad/comic-api:2.1.0 registry.local613.local:5000/kkdad/comic-api:2.1.0
docker push registry.local613.local:5000/kkdad/comic-api:2.1.0

helm upgrade comics comics
```

#### API Documentation
To view the API:
- Swagger UI: https://comics.gilbert.ca/swagger-ui/index.html
- API Endpoint: https://comics.gilbert.ca/api/v1/comics
- OpenAPI JSON: https://comics.gilbert.ca/v3/api-docs

### ComicViewer

The ComicViewer is an Angular-based frontend that provides a user-friendly interface for browsing cached comics.

#### Local Development
```bash
# Navigate to ComicViewer directory
cd ComicViewer

# Install dependencies
npm install

# Run development server (available at http://localhost:4200)
ng serve

# Run tests
ng test

# Build for production
npm run buildProd
```

#### Docker Deployment
```bash
# Build Docker image
cd ComicViewer
./build-docker.sh
docker tag kkdad/comic-viewer:latest registry.local613.local:5000/kkdad/comic-viewer:1.0.0
docker push registry.local613.local:5000/kkdad/comic-viewer:1.0.0
```

#### Features
- Material Design UI with responsive layout
- Infinite virtual scrolling for efficient comic viewing
- Automatic comic refresh with notification
- Date-based navigation for viewing historical comics

## Technology Stack

### Backend (ComicAPI)
- **Framework**: Spring Boot 3.4.5
- **Language**: Java 21
- **API Documentation**: OpenAPI 3.0 with springdoc-openapi-starter-webmvc-ui
- **Build Tool**: Gradle 8.5
- **Testing**: JUnit 5, Spring Test
- **Containerization**: Docker

### Frontend (ComicViewer)
- **Framework**: Angular with Material Design components
- **Build Tool**: Angular CLI
- **Testing**: Karma, Jasmine
- **Containerization**: Docker

### Deployment
- Kubernetes (K8s) with Helm charts
- Automated builds with GitHub Actions

## Resources

- **Material Design Principles**: https://material.io/design/components/cards.html#usage
- **Material Design in Angular**: https://material.angular.io/components/categories
- **JSON to TypeScript Converter**: http://json2ts.com/
- **Angular Virtual Scrolling**: https://material.angular.io/cdk/scrolling/overview
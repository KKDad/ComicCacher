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
- Comprehensive metrics for cache performance and storage usage
- Per-comic storage and access analytics
- Guaranteed once-daily execution tracking for startup procedures and downloads
- Scheduled daily reconciliation of comic configuration at configurable times

### API Features
- RESTful endpoints for comic retrieval and management
- Standardized API responses with consistent formatting
- Proper exception handling with informative error messages
- Optional filtering by comic name, date, and other parameters
- Metrics API for monitoring cache efficiency and usage patterns

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

# Generate test coverage reports
./gradlew :ComicAPI:jacocoTestReport         # Unit test coverage only
./gradlew :ComicAPI:jacocoIntegrationTestReport   # Integration test coverage only
./gradlew :ComicAPI:jacocoAllReport          # Combined coverage report

# Generate API documentation
./gradlew :ComicAPI:updateApiDocs

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
- Metrics API: https://comics.gilbert.ca/api/v1/metrics
- OpenAPI JSON: https://comics.gilbert.ca/v3/api-docs

#### Metrics API Endpoints
The API provides several endpoints for monitoring cache performance:
- `/api/v1/metrics/storage` - Storage utilization by comic and year
- `/api/v1/metrics/access` - Access patterns, hit ratios, and timing data
- `/api/v1/metrics/combined` - Comprehensive view of storage and access metrics
- `/api/v1/metrics/storage/refresh` - Force refresh of storage metrics

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
- **Test Coverage**: JaCoCo with separate unit and integration test reporting
- **Containerization**: Docker

### Frontend (ComicViewer)
- **Framework**: Angular with Material Design components
- **Build Tool**: Angular CLI
- **Testing**: Karma, Jasmine
- **Containerization**: Docker

### Deployment
- Kubernetes (K8s) with Helm charts
- Automated builds with GitHub Actions

## Code Quality and Test Coverage

The project uses JaCoCo for test coverage reporting. Coverage reports are generated automatically when running tests:

- **Unit Test Coverage**: `./gradlew :ComicAPI:jacocoTestReport`
- **Integration Test Coverage**: `./gradlew :ComicAPI:jacocoIntegrationTestReport`
- **Combined Coverage**: `./gradlew :ComicAPI:jacocoAllReport`

Coverage reports are generated in HTML and XML formats in the following locations:
- Unit Tests: `ComicAPI/build/reports/jacoco/test/html/index.html`
- Integration Tests: `ComicAPI/build/reports/jacoco/jacocoIntegrationTestReport/html/index.html`
- Combined: `ComicAPI/build/reports/jacoco/jacocoAllReport/html/index.html`

## API Documentation

The API documentation is automatically generated using the OpenAPI Specification (formerly known as Swagger):

- **Local Development**: When running locally, access the Swagger UI at `http://localhost:8080/swagger-ui/index.html`
- **Generated Files**: OpenAPI JSON files are generated in the `docs/` directory during the build process
- **Custom Generator**: Run `./gradlew :ComicAPI:updateApiDocs` to manually update the documentation

### Detailed Endpoint Documentation

Detailed documentation for all API endpoints is available in the `docs/` directory:

- [API Endpoints Overview](docs/api-endpoints.md) - Main index of all available endpoints
- [Comics Endpoints](docs/comics-endpoints.md) - Documentation for comic retrieval and management
- [Authentication Endpoints](docs/auth-endpoints.md) - User registration, login, and token management
- [Health Endpoint](docs/health-endpoint.md) - Application health status information
- [Metrics Endpoints](docs/metrics-endpoints.md) - Storage and access statistics
- [Preferences Endpoints](docs/preferences-endpoints.md) - User preferences management
- [Update Endpoints](docs/update-endpoints.md) - Comic update and retrieval
- [User Endpoints](docs/user-endpoints.md) - User profile management

## Resources

- **Material Design Principles**: https://material.io/design/components/cards.html#usage
- **Material Design in Angular**: https://material.angular.io/components/categories
- **JSON to TypeScript Converter**: http://json2ts.com/
- **Angular Virtual Scrolling**: https://material.angular.io/cdk/scrolling/overview
- **JaCoCo Documentation**: https://www.eclemma.org/jacoco/
- **OpenAPI/Swagger**: https://swagger.io/specification/
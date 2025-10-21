# Project Overview

This project, "The Comic Processor," is a webcomic scroller that consists of a Java-based backend (ComicAPI) and an Angular-based frontend (ComicViewer). The backend is responsible for caching comics from sources like GoComics and ComicsKingdom, while the frontend provides a user interface for browsing the cached comics.

## Backend (ComicAPI)

The backend is a Spring Boot application written in Java 21. It exposes a REST API for comic retrieval and management, with OpenAPI 3.0 documentation. The project is built using Gradle and includes support for unit and integration tests with JaCoCo for code coverage.

### Key Technologies:
- **Framework:** Spring Boot 3.4.5
- **Language:** Java 21
- **Build Tool:** Gradle 8.5
- **API Documentation:** OpenAPI 3.0 with springdoc-openapi-starter-webmvc-ui
- **Testing:** JUnit 5, Spring Test, JaCoCo

## Frontend (ComicViewer)

The frontend is an Angular application that uses Angular Material for its UI components. It provides features like infinite virtual scrolling and automatic comic refresh. The project is built using the Angular CLI and includes support for testing with Karma and Jasmine.

### Key Technologies:
- **Framework:** Angular with Material Design
- **Build Tool:** Angular CLI
- **Testing:** Karma, Jasmine

# Building and Running

## Backend (ComicAPI)

### Local Development
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

### Docker Deployment
```bash
# Build the project
./gradlew :ComicAPI:build

# Build the Docker image
./ComicAPI/build-docker.sh <version>
```

## Frontend (ComicViewer)

### Local Development
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

### Docker Deployment
```bash
# Navigate to ComicViewer directory
cd ComicViewer

# Build Docker image
./build-docker.sh
```

# Development Conventions

## Backend (ComicAPI)

*   The project follows standard Java and Spring Boot conventions.
*   Unit and integration tests are located in `src/test/java` and `src/integration/java` respectively.
*   JaCoCo is used for code coverage, and reports can be generated using the `jacocoTestReport`, `jacocoIntegrationTestReport`, and `jacocoAllReport` Gradle tasks.
*   API documentation is generated using the `springdoc-openapi-gradle-plugin` and can be updated by running the `updateApiDocs` Gradle task.

## Frontend (ComicViewer)

*   The project follows standard Angular conventions.
*   Tests are written using Karma and Jasmine and can be run with the `ng test` command.
*   The `angular.json` file contains the project's build and test configurations.
*   The `package.json` file lists the project's dependencies and scripts.

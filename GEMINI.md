# Project Overview

This project, "The Comic Processor," is a webcomic scroller that consists of a Java-based backend (comic-api) and an Angular-based frontend (comic-web). The backend is responsible for caching comics from sources like GoComics and ComicsKingdom, while the frontend provides a user interface for browsing the cached comics.

## Backend (comic-api)

The backend is a Spring Boot application written in Java 21. It exposes a REST API for comic retrieval and management, with OpenAPI 3.0 documentation. The project is built using Gradle and includes support for unit and integration tests with JaCoCo for code coverage.

### Key Technologies:
- **Framework:** Spring Boot 3.4.5
- **Language:** Java 21
- **Build Tool:** Gradle 8.5
- **API Documentation:** OpenAPI 3.0 with springdoc-openapi-starter-webmvc-ui
- **Testing:** JUnit 5, Spring Test, JaCoCo

## Frontend (comic-web)

The frontend is an Angular application that uses Angular Material for its UI components. It provides features like infinite virtual scrolling and automatic comic refresh. The project is built using the Angular CLI and includes support for testing with Karma and Jasmine.

### Key Technologies:
- **Framework:** Angular with Material Design
- **Build Tool:** Angular CLI
- **Testing:** Karma, Jasmine

# Building and Running

## Backend (comic-api)

### Local Development
```bash
# Build the project
./gradlew :comic-api:build

# Run tests
./gradlew :comic-api:test

# Run integration tests
./gradlew :comic-api:integrationTest

# Generate test coverage reports
./gradlew :comic-api:jacocoTestReport         # Unit test coverage only
./gradlew :comic-api:jacocoIntegrationTestReport   # Integration test coverage only
./gradlew :comic-api:jacocoAllReport          # Combined coverage report

# Generate API documentation
./gradlew :comic-api:updateApiDocs

# Run the application locally
./gradlew :comic-api:bootRun
```

### Docker Deployment
```bash
# Build the project
./gradlew :comic-api:build

# Build the Docker image
./comic-api/build-docker.sh <version>
```

## Frontend (comic-web)

### Local Development
```bash
# Navigate to comic-web directory
cd comic-web

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
# Navigate to comic-web directory
cd comic-web

# Build Docker image
./build-docker.sh
```

# Development Conventions

## Backend (comic-api)

*   The project follows standard Java and Spring Boot conventions.
*   Unit and integration tests are located in `src/test/java` and `src/integration/java` respectively.
*   JaCoCo is used for code coverage, and reports can be generated using the `jacocoTestReport`, `jacocoIntegrationTestReport`, and `jacocoAllReport` Gradle tasks.
*   API documentation is generated using the `springdoc-openapi-gradle-plugin` and can be updated by running the `updateApiDocs` Gradle task.

## Frontend (comic-web)

*   The project follows standard Angular conventions.
*   Tests are written using Karma and Jasmine and can be run with the `ng test` command.
*   The `angular.json` file contains the project's build and test configurations.
*   The `package.json` file lists the project's dependencies and scripts.

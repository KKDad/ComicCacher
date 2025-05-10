# API Documentation

This directory contains automatically generated API documentation for the ComicCacher project.

## OpenAPI Documentation

The OpenAPI JSON file is automatically generated during the build process and placed here. This file can be used with tools like Swagger UI to visualize and interact with the API's resources.

## How to Use

You can view the API documentation in several ways:

1. **Using the built-in Swagger UI**: When the application is running, navigate to `/swagger-ui.html` to use the interactive documentation.

2. **Using the OpenAPI JSON file**: The `openapi.json` file can be imported into tools like:
   - [Swagger Editor](https://editor.swagger.io/)
   - [Postman](https://www.postman.com/)
   - [Insomnia](https://insomnia.rest/)

## Automated Updates

The API documentation is automatically updated during the build process using the following Gradle task:

```bash
./gradlew updateApiDocs
```

This task is also triggered as part of the regular build process.
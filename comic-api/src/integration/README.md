# Integration Tests

Spring Boot integration tests for comic-api, run with the `com.coditory.integration-test` Gradle plugin. Test classes end in `IT` and live under `src/integration/java`.

## Running

```bash
./gradlew :comic-api:integrationTest    # integration tests only
./gradlew clean testAll                 # unit + integration tests with coverage checks
```

`integrationTest` runs with the `integration` Spring profile. The integration resources (`src/integration/resources`) come before `src/main/resources` on the classpath, so `application-integration.properties` and `logback-test.xml` override the main ones.

## Base Classes

| Class | Use for |
|-------|---------|
| `AbstractIntegrationTest` | Full application on a random port with `MockMvc`, `integration` profile |
| `AbstractHttpGraphQlIntegrationTest` | GraphQL resolver tests over HTTP (`*ResolverIT`) |
| `batch.AbstractBatchJobIntegrationTest` | Spring Batch job tests with the `batch-integration` profile and generated test images |

## Live-Network Tests

`downloader.GoComicsIntegrationIT` and `downloader.ComicsKingdomIntegrationIT` fetch from the real sites, so they can fail when a site changes or rate limits, independent of the code under test. Both exercise the legacy `IDailyComic` classes (`GoComics` uses Selenium, `ComicsKingdom` Jsoup), not the production `*DownloaderStrategy` classes; see "Replace the Selenium GoComics IT" in `TODO.md`.

package org.stapledon.engine.downloader;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.openqa.selenium.By;
import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;
import org.stapledon.common.config.CacheProperties;
import org.stapledon.common.dto.ComicItem;
import org.stapledon.common.infrastructure.web.WebInspector;

import java.io.File;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.Random;

import io.github.bonigarcia.wdm.WebDriverManager;
import lombok.ToString;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@ToString
public class GoComics extends DailyComic implements AutoCloseable {

    private WebDriver driver;
    private final CacheProperties cacheProperties;
    private final Random random = new Random();
    private final List<String> userAgents = Arrays.asList(
        "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/108.0.0.0 Safari/537.36",
        "Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/108.0.0.0 Safari/537.36",
        "Mozilla/5.0 (X11; Linux x86_64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/108.0.0.0 Safari/537.36",
        "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Edge/108.0.1462.46 Safari/537.36",
        "Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) AppleWebKit/537.36 (KHTML, like Gecko) Firefox/108.0"
    );

    private String getRandomUserAgent() {
        return userAgents.get(random.nextInt(userAgents.size()));
    }

    private void initializeWebDriver() {
        if (driver != null) {
            return; // Already initialized
        }

        log.debug("Initializing WebDriver for GoComics downloader");
        WebDriverManager.chromedriver().setup();
        ChromeOptions options = new ChromeOptions();

        // Configure headless mode based on application properties
        if (cacheProperties.isChromeHeadless()) {
            log.debug("Running Chrome in headless mode");
            options.addArguments("--headless");
        } else {
            log.debug("Running Chrome with GUI (headed mode)");
        }

        options.addArguments("--disable-gpu");
        options.addArguments("--window-size=1920,1080");
        options.addArguments("--no-sandbox");
        options.addArguments("--disable-dev-shm-usage");
        options.addArguments("--user-agent=" + getRandomUserAgent());
        options.setExperimentalOption("excludeSwitches", Arrays.asList("enable-automation"));
        options.setExperimentalOption("useAutomationExtension", false);

        driver = new ChromeDriver(options);
    }

    /**
     * Ensures WebDriver is initialized before use (lazy initialization)
     */
    private void ensureWebDriverInitialized() {
        if (driver == null) {
            initializeWebDriver();
        }
    }

    private void quitWebDriver() {
        if (driver != null) {
            driver.quit();
            driver = null;
        }
    }

    @Override
    public void close() {
        quitWebDriver();
    }

    public GoComics(WebInspector inspector, CacheProperties cacheProperties) {
        super(inspector, "[src]");
        this.cacheProperties = cacheProperties;
        // WebDriver initialization moved to lazy init - only create when actually needed
    }


    /**
     * Determines when the latest published image it. Some comics are only available on the web a couple days or
     * a week after they were published in print.
     *
     * @return Mst recent date we can get a api for
     */
    public LocalDate getLastStripOn() {
        return LocalDate.now();
    }

    /**
     * Generate the URL for a specific date.
     */
    @Override
    protected String generateSiteURL() {
        return String.format("https://www.gocomics.com/%s/%s/", this.comicNameParsed, this.currentDate.format(DateTimeFormatter.ofPattern("yyyy/MM/dd"))).toLowerCase();
    }

    /**
     * Link to the About the comic page
     *
     * @return URL where we can get the 'about' information for this strip
     */
    private String generateAboutURL() {
        return String.format("https://www.gocomics.com/%s/about", this.comicNameParsed);
    }

    /**
     * Updates the metadata for a comic strip by fetching information from the GoComics website.
     * <p>
     * The method attempts to extract author information and description from the comic's page,
     * looking for structured data in JSON-LD format. If this fails, it will use a generic
     * approach to try to find the information elsewhere on the page.
     * <p>
     * Due to the website's structure potentially changing over time, this method is designed
     * to gracefully degrade by providing sensible defaults when information can't be extracted.
     *
     * @param comicItem The ComicItem to update with metadata from the web
     */
    public void updateComicMetadata(ComicItem comicItem) {
        try {
            ensureWebDriverInitialized();

            String url = this.generateSiteURL();
            log.info("Getting comic metadata from {}", url);

            driver.get(url);

            // Add random delay to simulate human behavior
            Thread.sleep(random.nextInt(2000) + 1000); // 1-3 seconds delay

            // Execute JavaScript to spoof navigator.webdriver
            ((JavascriptExecutor) driver).executeScript("Object.defineProperty(navigator, 'webdriver', {get: () => undefined})");

            // Get the page source after JavaScript execution
            Document doc = Jsoup.parse(driver.getPageSource());

            // Set default values in case extraction fails
            comicItem.setDescription("A comic strip published on GoComics");
            comicItem.setAuthor("By " + inferAuthorFromComicName(this.comicNameParsed));

            // Try multiple extraction methods in order of preference
            boolean authorFound = extractFromJsonLd(doc, comicItem) ||
                                  extractFromMetaTags(doc, comicItem) ||
                                  extractFromPageTitle(doc, comicItem);

            // Set avatar available regardless
            comicItem.setAvatarAvailable(true);

            // Cache the avatar if we don't already have it
            var avatarCached = new File(String.format("%s/avatar.png", this.cacheLocation()));
            if (!avatarCached.exists()) {
                // Try to find badge image in HTML using different potential CSS classes
                Element badgeImage = doc.select("img.Badge_badge__image__Y3HaD, img[src*=badge], img[src*=avatar]").first();
                if (badgeImage != null) {
                    // Use the new cacheImage method that accepts a URL string
                    comicItem.setAvatarAvailable(cacheImage(badgeImage.attr("abs:src"), avatarCached.getAbsolutePath()));
                    log.trace("Avatar has been cached");
                }
            }
        } catch (Exception e) {
            log.error("Error updating comic metadata: {}", e.getMessage());
            // Ensure we always have valid data even after errors
            if (comicItem.getDescription() == null) {
                comicItem.setDescription("A comic strip published on GoComics");
            }
            if (comicItem.getAuthor() == null) {
                comicItem.setAuthor("By " + inferAuthorFromComicName(this.comicNameParsed));
            }
            comicItem.setAvatarAvailable(true);
        }
    }

    /**
     * Attempts to extract comic metadata from JSON-LD structured data in the page
     */
    private boolean extractFromJsonLd(Document doc, ComicItem comicItem) {
        try {
            Elements jsonScripts = doc.select("script[type=application/ld+json]");

            for (Element script : jsonScripts) {
                String json = script.html();

                // Look for author information in JSON-LD
                if (json.contains("\"author\":{") && json.contains("\"name\":")) {
                    // Extract author name
                    int nameStartIndex = json.indexOf("\"name\":\"", json.indexOf("\"author\":{")) + 8;
                    if (nameStartIndex > 8) {
                        int nameEndIndex = json.indexOf("\"", nameStartIndex);
                        if (nameEndIndex > nameStartIndex) {
                            String authorName = json.substring(nameStartIndex, nameEndIndex);
                            comicItem.setAuthor("By " + authorName);

                            // Also try to extract description
                            int descStartIndex = json.indexOf("\"description\":\"") + 14;
                            if (descStartIndex > 14) {
                                int descEndIndex = json.indexOf("\"", descStartIndex);
                                if (descEndIndex > descStartIndex) {
                                    String description = json.substring(descStartIndex, descEndIndex);
                                    comicItem.setDescription(description);
                                }
                            }
                            return true;
                        }
                    }
                }
            }
        } catch (Exception e) {
            log.debug("Failed to extract metadata from JSON-LD: {}", e.getMessage());
        }
        return false;
    }

    /**
     * Attempts to extract comic metadata from meta tags in the page header
     */
    private boolean extractFromMetaTags(Document doc, ComicItem comicItem) {
        try {
            // Try to get author from meta tags
            Element authorMeta = doc.select("meta[name=author], meta[property=og:author]").first();
            if (authorMeta != null) {
                String authorName = authorMeta.attr("content");
                if (authorName != null && !authorName.isEmpty()) {
                    comicItem.setAuthor("By " + authorName);

                    // Also try to get description
                    Element descMeta = doc.select("meta[name=description], meta[property=og:description]").first();
                    if (descMeta != null) {
                        String desc = descMeta.attr("content");
                        if (desc != null && !desc.isEmpty()) {
                            comicItem.setDescription(desc);
                        }
                    }
                    return true;
                }
            }
        } catch (Exception e) {
            log.debug("Failed to extract metadata from meta tags: {}", e.getMessage());
        }
        return false;
    }

    /**
     * Attempts to extract comic metadata from the page title
     */
    private boolean extractFromPageTitle(Document doc, ComicItem comicItem) {
        try {
            Element titleElement = doc.select("title").first();
            if (titleElement != null) {
                String title = titleElement.text();
                // Try to extract author name from title if it follows patterns like "Comic Name by Author Name"
                if (title.contains(" by ")) {
                    String authorName = title.substring(title.lastIndexOf(" by ") + 4).trim();
                    comicItem.setAuthor("By " + authorName);
                    return true;
                }
            }
        } catch (Exception e) {
            log.debug("Failed to extract metadata from page title: {}", e.getMessage());
        }
        return false;
    }

    /**
     * Infers a likely author name based on the comic name
     * Used as a last resort when online extraction fails
     */
    private String inferAuthorFromComicName(String comicName) {
        if (comicName == null) {
            return "Unknown Creator";
        }

        // Format comic name to create a more human-readable default
        String formattedName = comicName.replace("-", " ")
                                       .replaceAll("([a-z])([A-Z])", "$1 $2");

        return "Creator of " + formattedName;
    }

    @Override
    protected Optional<String> extractComicImage(String comicUrl) {
        try {
            ensureWebDriverInitialized();

            driver.get(comicUrl);

            // Add random delay to simulate human behavior
            Thread.sleep(random.nextInt(2000) + 1000); // 1-3 seconds delay

            // Execute JavaScript to spoof navigator.webdriver
            ((JavascriptExecutor) driver).executeScript("Object.defineProperty(navigator, 'webdriver', {get: () => undefined})");

            // === DIAGNOSTIC LOGGING: Inspect the actual rendered DOM ===
            JavascriptExecutor jsExecutor = (JavascriptExecutor) driver;

            // Get page title to confirm we're on the right page
            String pageTitle = (String) jsExecutor.executeScript("return document.title;");
            log.info("Page title: {}", pageTitle);

            // Get body HTML (first 2000 chars to avoid overwhelming logs)
            String bodyHtml = (String) jsExecutor.executeScript("return document.body.innerHTML.substring(0, 2000);");
            log.debug("Body HTML (first 2000 chars): {}", bodyHtml);

            // Get all img tags with their attributes
            Object imgResult = jsExecutor.executeScript(
                "var imgs = document.querySelectorAll('img');" +
                "var result = [];" +
                "for(var i = 0; i < imgs.length; i++) {" +
                "  result.push({" +
                "    src: imgs[i].src," +
                "    className: imgs[i].className," +
                "    id: imgs[i].id," +
                "    alt: imgs[i].alt" +
                "  });" +
                "}" +
                "return JSON.stringify(result);"
            );
            log.info("All img tags found: {}", imgResult);

            // Get all meta tags
            Object metaResult = jsExecutor.executeScript(
                "var metas = document.querySelectorAll('meta[property], meta[name]');" +
                "var result = [];" +
                "for(var i = 0; i < metas.length; i++) {" +
                "  result.push({" +
                "    property: metas[i].getAttribute('property')," +
                "    name: metas[i].getAttribute('name')," +
                "    content: metas[i].content" +
                "  });" +
                "}" +
                "return JSON.stringify(result);"
            );
            log.info("All meta tags found: {}", metaResult);

            // Extract comic image using CSS selector for current GoComics structure
            List<org.openqa.selenium.WebElement> imgElements = driver.findElements(By.cssSelector("img[class*='Comic_comic__image']"));

            if (!imgElements.isEmpty()) {
                String src = imgElements.get(0).getAttribute("src");
                if (src != null && !src.isEmpty()) {
                    log.info("Found comic image: {}", src);
                    return Optional.of(src);
                }
            }

            log.warn("No comic image found");
            return Optional.empty();
        } catch (Exception e) {
            log.error("Error extracting comic image using WebDriver: {}", e.getMessage(), e);
            return Optional.empty();
        }
    }


}

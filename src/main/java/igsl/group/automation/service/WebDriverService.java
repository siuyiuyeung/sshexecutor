package igsl.group.automation.service;

import io.github.bonigarcia.wdm.WebDriverManager;
import lombok.extern.slf4j.Slf4j;
import org.openqa.selenium.By;
import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.NoSuchElementException;
import org.openqa.selenium.OutputType;
import org.openqa.selenium.PageLoadStrategy;
import org.openqa.selenium.TakesScreenshot;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

@Service
@Slf4j
public class WebDriverService {

    @Value("${automation.screenshot.path:screenshots}")
    private String screenshotPath;

    @Value("${automation.driver.headless:false}")
    private boolean headless;

    private static final DateTimeFormatter TIMESTAMP_FORMAT = DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss");

    public WebDriver createDriver() {
        // Suppress CDP version warnings
        Logger.getLogger("org.openqa.selenium").setLevel(Level.WARNING);
        System.setProperty("webdriver.chrome.silentOutput", "true");

        // Setup ChromeDriver using WebDriverManager
        WebDriverManager.chromedriver().setup();

        ChromeOptions options = new ChromeOptions();

        if (headless) {
            options.addArguments("--headless=new");
        }

        // Common Chrome options for stability
        options.addArguments("--no-sandbox");
        options.addArguments("--disable-dev-shm-usage");
        options.addArguments("--disable-blink-features=AutomationControlled");
        options.addArguments("--disable-extensions");
        options.addArguments("--disable-gpu");
        options.addArguments("--disable-web-security");
        options.addArguments("--window-size=1920,1080");

        // Disable automation info bar
        options.setExperimentalOption("excludeSwitches", new String[]{"enable-automation"});
        options.setExperimentalOption("useAutomationExtension", false);

        // Prefs to disable notifications and other popups
        Map<String, Object> prefs = new HashMap<>();
        prefs.put("credentials_enable_service", false);
        prefs.put("profile.password_manager_enabled", false);
        prefs.put("profile.default_content_setting_values.notifications", 2);
        options.setExperimentalOption("prefs", prefs);

        // Set page load strategy
        options.setPageLoadStrategy(PageLoadStrategy.NORMAL);

        // Disable logs
        options.addArguments("--log-level=3");
        options.addArguments("--silent");

        return new ChromeDriver(options);
    }

    public String captureScreenshot(WebDriver driver, String selector, String configName) throws Exception {
        return captureScreenshot(driver, selector, configName, null);
    }

    public String captureScreenshot(WebDriver driver, String selector, String configName, Integer stepNumber) throws Exception {
        // Sanitize config name for filename and directory
        String sanitizedConfigName = configName.replaceAll("[^a-zA-Z0-9-_]", "_");

        // Generate timestamp
        String timestamp = LocalDateTime.now().format(TIMESTAMP_FORMAT);

        // Build filename
        StringBuilder fileNameBuilder = new StringBuilder();
        fileNameBuilder.append(sanitizedConfigName);
        fileNameBuilder.append("_");
        fileNameBuilder.append(timestamp);

        if (stepNumber != null) {
            fileNameBuilder.append("_step");
            fileNameBuilder.append(stepNumber);
        }

        fileNameBuilder.append(".png");

        String fileName = fileNameBuilder.toString();

        // Create subdirectory for the configuration
        Path configDir = Paths.get(screenshotPath, sanitizedConfigName);
        Files.createDirectories(configDir);

        Path path = configDir.resolve(fileName);

        if (selector != null && !selector.isEmpty()) {
            try {
                WebElement element = driver.findElement(By.cssSelector(selector));
                // Scroll element into view before taking screenshot
                ((JavascriptExecutor) driver).executeScript("arguments[0].scrollIntoView(true);", element);
                Thread.sleep(500); // Small delay to ensure element is in view

                File screenshot = element.getScreenshotAs(OutputType.FILE);
                Files.copy(screenshot.toPath(), path);
            } catch (NoSuchElementException e) {
                // If element not found, take full page screenshot
                log.warn("Element not found with selector: {}. Taking full page screenshot.", selector);
                TakesScreenshot takesScreenshot = (TakesScreenshot) driver;
                File screenshot = takesScreenshot.getScreenshotAs(OutputType.FILE);
                Files.copy(screenshot.toPath(), path);
            }
        } else {
            TakesScreenshot takesScreenshot = (TakesScreenshot) driver;
            File screenshot = takesScreenshot.getScreenshotAs(OutputType.FILE);
            Files.copy(screenshot.toPath(), path);
        }

        log.info("Screenshot saved: {}", path);
        return path.toString();
    }

    public void waitForElement(WebDriver driver, String selector, int timeout) {
        WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(timeout));
        wait.until(ExpectedConditions.presenceOfElementLocated(By.cssSelector(selector)));
    }

    public void waitForElementClickable(WebDriver driver, String selector, int timeout) {
        WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(timeout));
        wait.until(ExpectedConditions.elementToBeClickable(By.cssSelector(selector)));
    }
}
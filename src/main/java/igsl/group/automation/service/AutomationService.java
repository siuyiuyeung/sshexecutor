package igsl.group.automation.service;

import igsl.group.automation.entity.AutomationConfig;
import igsl.group.automation.entity.AutomationResult;
import igsl.group.automation.entity.AutomationStep;
import igsl.group.automation.repository.AutomationResultRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.openqa.selenium.By;
import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.ui.Select;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.ArrayList;

@Service
@RequiredArgsConstructor
@Slf4j
public class AutomationService {

    private final WebDriverService webDriverService;
    private final AutomationResultRepository resultRepository;

    public AutomationResult executeAutomation(AutomationConfig config) {
        AutomationResult result = new AutomationResult();
        result.setConfig(config);
        result.setStartTime(LocalDateTime.now());
        result.setStatus(AutomationResult.Status.RUNNING);
        result.setScreenshotPaths(new ArrayList<>());

        StringBuilder logs = new StringBuilder();
        WebDriver driver = null;

        try {
            driver = webDriverService.createDriver();
            logs.append("Driver initialized\n");

            for (AutomationStep step : config.getSteps()) {
                executeStep(driver, step, result, logs);
            }

            result.setStatus(AutomationResult.Status.SUCCESS);
            logs.append("Automation completed successfully\n");

        } catch (Exception e) {
            result.setStatus(AutomationResult.Status.FAILED);
            result.setErrorMessage(e.getMessage());
            logs.append("Error: ").append(e.getMessage()).append("\n");
            log.error("Automation failed for config: " + config.getName(), e);
        } finally {
            if (driver != null) {
                driver.quit();
            }
            result.setEndTime(LocalDateTime.now());
            result.setLogs(logs.toString());
            resultRepository.save(result);
        }

        return result;
    }

    private void executeStep(WebDriver driver, AutomationStep step,
                             AutomationResult result, StringBuilder logs) throws Exception {
        logs.append("Executing step: ").append(step.getType()).append("\n");

        // Validate step data
        validateStep(step);

        // Get configuration name for screenshot naming
        String configName = result.getConfig().getName();
        int stepIndex = step.getOrder() + 1; // Make it 1-based for user readability

        switch (step.getType()) {
            case NAVIGATE:
                String url = step.getValue();
                if (url == null || url.trim().isEmpty()) {
                    throw new IllegalArgumentException("Navigate step requires a valid URL");
                }
                // Add protocol if missing
                if (!url.startsWith("http://") && !url.startsWith("https://")) {
                    url = "https://" + url;
                }
                driver.get(url);
                logs.append("Navigated to: ").append(url).append("\n");
                break;

            case CLICK:
                if (step.getSelector() == null || step.getSelector().trim().isEmpty()) {
                    throw new IllegalArgumentException("Click step requires a selector");
                }
                WebElement clickElement = driver.findElement(By.cssSelector(step.getSelector()));
                clickElement.click();
                logs.append("Clicked element: ").append(step.getSelector()).append("\n");
                break;

            case INPUT:
                if (step.getSelector() == null || step.getSelector().trim().isEmpty()) {
                    throw new IllegalArgumentException("Input step requires a selector");
                }
                if (step.getValue() == null) {
                    step.setValue(""); // Allow empty input
                }
                WebElement inputElement = driver.findElement(By.cssSelector(step.getSelector()));
                inputElement.clear();
                inputElement.sendKeys(step.getValue());
                logs.append("Input text to: ").append(step.getSelector()).append("\n");
                break;

            case WAIT:
                int waitTime = step.getWaitSeconds();
                if (waitTime <= 0) {
                    waitTime = 1; // Default to 1 second minimum
                }
                Thread.sleep(waitTime * 1000L);
                logs.append("Waited for: ").append(waitTime).append(" seconds\n");
                break;

            case SCREENSHOT:
                String screenshotPath = webDriverService.captureScreenshot(
                        driver, step.getCaptureSelector(), configName, stepIndex);
                result.getScreenshotPaths().add(screenshotPath);
                logs.append("Screenshot captured: ").append(screenshotPath).append("\n");
                break;

            case SCROLL:
                String scrollValue = step.getValue() != null ? step.getValue() : "0";
                ((JavascriptExecutor) driver).executeScript("window.scrollTo(0, " + scrollValue + ")");
                logs.append("Scrolled to position: ").append(scrollValue).append("\n");
                break;

            case SELECT:
                if (step.getSelector() == null || step.getSelector().trim().isEmpty()) {
                    throw new IllegalArgumentException("Select step requires a selector");
                }
                if (step.getValue() == null || step.getValue().trim().isEmpty()) {
                    throw new IllegalArgumentException("Select step requires a value");
                }
                Select select = new Select(driver.findElement(By.cssSelector(step.getSelector())));
                select.selectByValue(step.getValue());
                logs.append("Selected option: ").append(step.getValue()).append("\n");
                break;
        }

        if (step.isCaptureScreenshot() && step.getType() != AutomationStep.StepType.SCREENSHOT) {
            String screenshotPath = webDriverService.captureScreenshot(
                    driver, step.getCaptureSelector(), configName, stepIndex);
            result.getScreenshotPaths().add(screenshotPath);
            logs.append("Step screenshot captured: ").append(screenshotPath).append("\n");
        }

        if (step.getWaitSeconds() > 0 && step.getType() != AutomationStep.StepType.WAIT) {
            Thread.sleep(step.getWaitSeconds() * 1000L);
        }
    }

    private void validateStep(AutomationStep step) {
        if (step.getType() == null) {
            throw new IllegalArgumentException("Step type is required");
        }
    }
}
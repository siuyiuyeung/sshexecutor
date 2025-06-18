package igsl.group.automation.config;

import jakarta.annotation.PreDestroy;
import org.openqa.selenium.WebDriver;
import org.springframework.context.annotation.Configuration;

import java.util.ArrayList;
import java.util.List;

@Configuration
public class SeleniumConfig {
    
    private final List<WebDriver> activeDrivers = new ArrayList<>();
    
    public void registerDriver(WebDriver driver) {
        activeDrivers.add(driver);
    }
    
    public void unregisterDriver(WebDriver driver) {
        activeDrivers.remove(driver);
    }
    
    @PreDestroy
    public void cleanup() {
        activeDrivers.forEach(driver -> {
            try {
                driver.quit();
            } catch (Exception e) {
                // Ignore
            }
        });
    }
} 
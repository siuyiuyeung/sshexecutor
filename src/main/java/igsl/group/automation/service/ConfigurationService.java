package igsl.group.automation.service;

import igsl.group.automation.dto.AutomationConfigDTO;
import igsl.group.automation.entity.AutomationConfig;
import igsl.group.automation.entity.AutomationStep;
import igsl.group.automation.entity.ScheduleConfig;
import igsl.group.automation.repository.AutomationConfigRepository;
import igsl.group.automation.repository.AutomationResultRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class ConfigurationService {

    private final AutomationConfigRepository configRepository;
    private final AutomationResultRepository resultRepository;
    private final UiAutomationSchedulerService schedulerService;

    @Transactional
    public AutomationConfig updateConfiguration(Long configId, AutomationConfigDTO dto) {
        AutomationConfig config = configRepository.findById(configId)
                .orElseThrow(() -> new IllegalArgumentException("Configuration not found"));

        // Update basic fields
        config.setName(dto.getName());
        config.setDescription(dto.getDescription());
        config.setActive(dto.isActive());
        config.setUpdatedAt(LocalDateTime.now());

        // Update steps - clear and re-add to avoid orphan removal issue
        updateSteps(config, dto.getSteps());

        // Update schedule
        updateSchedule(config, dto.getSchedule());

        // Save configuration
        AutomationConfig saved = configRepository.save(config);

        // Reschedule if needed
        schedulerService.rescheduleAutomation(saved);

        return saved;
    }

    private void updateSteps(AutomationConfig config, List<AutomationStep> newSteps) {
        // Clear existing steps
        if (config.getSteps() == null) {
            config.setSteps(new ArrayList<>());
        } else {
            config.getSteps().clear();
        }

        // Add new steps
        if (newSteps != null) {
            for (int i = 0; i < newSteps.size(); i++) {
                AutomationStep step = new AutomationStep();
                AutomationStep source = newSteps.get(i);

                // Copy fields from DTO
                step.setOrder(i);
                step.setType(source.getType());
                step.setSelector(source.getSelector());
                step.setValue(source.getValue());
                step.setWaitSeconds(source.getWaitSeconds());
                step.setCaptureScreenshot(source.isCaptureScreenshot());
                step.setCaptureSelector(source.getCaptureSelector());
                step.setConfig(config);

                config.getSteps().add(step);
            }
        }
    }

    private void updateSchedule(AutomationConfig config, ScheduleConfig newSchedule) {
        if (newSchedule != null) {
            if (config.getSchedule() == null) {
                config.setSchedule(new ScheduleConfig());
            }
            config.getSchedule().setType(newSchedule.getType());
            config.getSchedule().setCronExpression(newSchedule.getCronExpression());
            config.getSchedule().setIntervalMinutes(newSchedule.getIntervalMinutes());
            config.getSchedule().setRunOnceAt(newSchedule.getRunOnceAt());
        } else {
            config.setSchedule(null);
        }
    }

    @Transactional
    public void deleteConfiguration(Long configId, boolean force) {
        AutomationConfig config = configRepository.findById(configId)
                .orElseThrow(() -> new IllegalArgumentException("Configuration not found"));

        // Unschedule first
        schedulerService.unscheduleAutomation(configId);

        if (force) {
            // Delete associated results and their screenshots
            resultRepository.findAll().stream()
                    .filter(result -> result.getConfig().getId().equals(configId))
                    .forEach(result -> {
                        // Delete screenshot files
                        if (result.getScreenshotPaths() != null) {
                            result.getScreenshotPaths().forEach(screenshotPath -> {
                                try {
                                    Path path = Paths.get(screenshotPath);
                                    Files.deleteIfExists(path);
                                    log.info("Deleted screenshot: " + screenshotPath);
                                } catch (Exception e) {
                                    log.error("Failed to delete screenshot: " + screenshotPath, e);
                                }
                            });
                        }
                    });

            // Delete all results for this config
            resultRepository.deleteByConfigId(configId);
        }

        // Delete the configuration
        configRepository.deleteById(configId);
        log.info("Deleted configuration: " + config.getName());
    }

    public long countResultsForConfig(Long configId) {
        return resultRepository.countByConfigId(configId);
    }
}
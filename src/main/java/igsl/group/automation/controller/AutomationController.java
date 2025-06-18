package igsl.group.automation.controller;

import igsl.group.automation.dto.AutomationConfigDTO;
import igsl.group.automation.entity.AutomationConfig;
import igsl.group.automation.entity.AutomationResult;
import igsl.group.automation.entity.AutomationStep;
import igsl.group.automation.entity.ScheduleConfig;
import igsl.group.automation.repository.AutomationConfigRepository;
import igsl.group.automation.service.AutomationService;
import igsl.group.automation.service.ConfigurationService;
import igsl.group.automation.service.UiAutomationSchedulerService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/automation")
@RequiredArgsConstructor
@Slf4j
public class AutomationController {

    private final AutomationConfigRepository configRepository;
    private final AutomationService automationService;
    private final UiAutomationSchedulerService schedulerService;
    private final ConfigurationService configurationService;

    @GetMapping("/configs")
    public List<AutomationConfig> getAllConfigs() {
        return configRepository.findAll();
    }

    @GetMapping("/configs/{id}")
    public ResponseEntity<AutomationConfig> getConfig(@PathVariable Long id) {
        return configRepository.findById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping("/configs")
    public ResponseEntity<AutomationConfig> createConfig(@RequestBody AutomationConfigDTO dto) {
        // Validate configuration
        validateConfiguration(dto);

        AutomationConfig config = new AutomationConfig();
        // Map DTO to entity
        config.setName(dto.getName());
        config.setDescription(dto.getDescription());
        config.setSteps(dto.getSteps());
        config.setSchedule(dto.getSchedule());
        config.setActive(dto.isActive());

        AutomationConfig saved = configRepository.save(config);

        if (saved.isActive() && saved.getSchedule() != null) {
            schedulerService.scheduleAutomation(saved);
        }

        return ResponseEntity.ok(saved);
    }

    @PutMapping("/configs/{id}")
    public ResponseEntity<AutomationConfig> updateConfig(@PathVariable Long id,
                                                         @RequestBody AutomationConfigDTO dto) {
        // Validate configuration
        validateConfiguration(dto);

        try {
            AutomationConfig updated = configurationService.updateConfiguration(id, dto);
            return ResponseEntity.ok(updated);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.notFound().build();
        } catch (Exception e) {
            log.error("Error updating configuration", e);
            return ResponseEntity.internalServerError().build();
        }
    }

    private void validateConfiguration(AutomationConfigDTO dto) {
        if (dto.getName() == null || dto.getName().trim().isEmpty()) {
            throw new IllegalArgumentException("Configuration name is required");
        }

        if (dto.getSteps() == null || dto.getSteps().isEmpty()) {
            throw new IllegalArgumentException("At least one step is required");
        }

        // Validate each step
        for (int i = 0; i < dto.getSteps().size(); i++) {
            AutomationStep step = dto.getSteps().get(i);
            if (step.getType() == null) {
                throw new IllegalArgumentException("Step " + (i + 1) + ": Type is required");
            }

            switch (step.getType()) {
                case NAVIGATE:
                    if (step.getValue() == null || step.getValue().trim().isEmpty()) {
                        throw new IllegalArgumentException("Step " + (i + 1) + " (NAVIGATE): URL is required");
                    }
                    break;
                case CLICK:
                case INPUT:
                case SELECT:
                    if (step.getSelector() == null || step.getSelector().trim().isEmpty()) {
                        throw new IllegalArgumentException("Step " + (i + 1) + " (" + step.getType() + "): Selector is required");
                    }
                    break;
            }
        }

        // Validate schedule if present
        if (dto.getSchedule() != null) {
            ScheduleConfig schedule = dto.getSchedule();
            switch (schedule.getType()) {
                case ONCE:
                    if (schedule.getRunOnceAt() == null || schedule.getRunOnceAt().trim().isEmpty()) {
                        throw new IllegalArgumentException("Schedule: Run once date/time is required");
                    }
                    break;
                case INTERVAL:
                    if (schedule.getIntervalMinutes() == null || schedule.getIntervalMinutes() <= 0) {
                        throw new IllegalArgumentException("Schedule: Interval must be greater than 0");
                    }
                    break;
                case CRON:
                    if (schedule.getCronExpression() == null || schedule.getCronExpression().trim().isEmpty()) {
                        throw new IllegalArgumentException("Schedule: Cron expression is required");
                    }
                    break;
            }
        }
    }

    @DeleteMapping("/configs/{id}")
    public ResponseEntity<Map<String, Object>> deleteConfig(
            @PathVariable Long id,
            @RequestParam(defaultValue = "false") boolean force) {

        if (!configRepository.existsById(id)) {
            return ResponseEntity.notFound().build();
        }

        // Check if there are associated results
        long resultCount = configurationService.countResultsForConfig(id);

        if (resultCount > 0 && !force) {
            Map<String, Object> response = new HashMap<>();
            response.put("error", "Cannot delete configuration");
            response.put("message", String.format(
                    "This configuration has %d execution results. " +
                            "Delete the results first or use force=true to delete everything.",
                    resultCount));
            response.put("resultCount", resultCount);
            return ResponseEntity.status(HttpStatus.CONFLICT).body(response);
        }

        try {
            configurationService.deleteConfiguration(id, force);

            Map<String, Object> response = new HashMap<>();
            response.put("message", "Configuration deleted successfully");
            response.put("deletedResults", force ? resultCount : 0);

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            Map<String, Object> response = new HashMap<>();
            response.put("error", "Failed to delete configuration");
            response.put("message", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }

    @PostMapping("/configs/{id}/run")
    public ResponseEntity<AutomationResult> runNow(@PathVariable Long id) {
        return configRepository.findById(id)
                .map(config -> {
                    AutomationResult result = automationService.executeAutomation(config);
                    return ResponseEntity.ok(result);
                })
                .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/configs/{id}/toggle")
    public ResponseEntity<Map<String, Object>> getScheduleStatus(@PathVariable Long id) {
        return configRepository.findById(id)
                .map(config -> {
                    Map<String, Object> status = new HashMap<>();
                    status.put("configId", id);
                    status.put("configName", config.getName());
                    status.put("isActive", config.isActive());
                    status.put("isScheduled", schedulerService.isScheduled(id));
                    status.put("schedule", config.getSchedule());
                    return ResponseEntity.ok(status);
                })
                .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping("/configs/{id}/toggle")
    public ResponseEntity<AutomationConfig> toggleActive(@PathVariable Long id) {
        return configRepository.findById(id)
                .map(config -> {
                    config.setActive(!config.isActive());
                    AutomationConfig saved = configRepository.save(config);

                    if (saved.isActive() && saved.getSchedule() != null) {
                        schedulerService.scheduleAutomation(saved);
                    } else {
                        schedulerService.unscheduleAutomation(id);
                    }

                    return ResponseEntity.ok(saved);
                })
                .orElse(ResponseEntity.notFound().build());
    }
}
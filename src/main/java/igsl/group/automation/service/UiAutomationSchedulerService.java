package igsl.group.automation.service;

import igsl.group.automation.entity.AutomationConfig;
import igsl.group.automation.entity.ScheduleConfig;
import igsl.group.automation.repository.AutomationConfigRepository;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.scheduling.support.CronExpression;
import org.springframework.scheduling.support.CronTrigger;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ScheduledFuture;

@Service
@Slf4j
public class UiAutomationSchedulerService {

    private final TaskScheduler taskScheduler;
    private final AutomationService automationService;
    private final AutomationConfigRepository configRepository;

    // Map to store scheduled tasks
    private final Map<Long, ScheduledFuture<?>> scheduledTasks = new ConcurrentHashMap<>();

    public UiAutomationSchedulerService(@Qualifier("uiAutomationTaskScheduler") TaskScheduler taskScheduler,
                                        AutomationService automationService,
                                        AutomationConfigRepository configRepository) {
        this.taskScheduler = taskScheduler;
        this.automationService = automationService;
        this.configRepository = configRepository;
    }

    @PostConstruct
    public void init() {
        log.info("Initializing scheduler service");
        scheduleActiveAutomations();
    }

    @PreDestroy
    public void cleanup() {
        log.info("Cleaning up scheduled tasks");
        scheduledTasks.values().forEach(task -> task.cancel(false));
        scheduledTasks.clear();
    }

    public void scheduleActiveAutomations() {
        configRepository.findByActiveTrue().forEach(this::scheduleAutomation);
    }

    public void scheduleAutomation(AutomationConfig config) {
        if (!config.isActive() || config.getSchedule() == null) {
            return;
        }

        // Cancel existing schedule if any
        unscheduleAutomation(config.getId());

        try {
            ScheduledFuture<?> scheduledTask = null;
            ScheduleConfig schedule = config.getSchedule();

            Runnable task = () -> {
                log.info("Executing scheduled automation: " + config.getName());
                automationService.executeAutomation(config);
            };

            switch (schedule.getType()) {
                case ONCE:
                    LocalDateTime runTime = LocalDateTime.parse(schedule.getRunOnceAt());
                    Instant instant = runTime.atZone(ZoneId.systemDefault()).toInstant();
                    scheduledTask = taskScheduler.schedule(task, instant);
                    log.info("Scheduled one-time automation '{}' at {}", config.getName(), runTime);
                    break;

                case INTERVAL:
                    Duration interval = Duration.ofMinutes(schedule.getIntervalMinutes());
                    scheduledTask = taskScheduler.scheduleAtFixedRate(task, Instant.now(), interval);
                    log.info("Scheduled interval automation '{}' every {} minutes",
                            config.getName(), schedule.getIntervalMinutes());
                    break;

                case CRON:
                    if (isValidCronExpression(schedule.getCronExpression())) {
                        CronTrigger cronTrigger = new CronTrigger(schedule.getCronExpression());
                        scheduledTask = taskScheduler.schedule(task, cronTrigger);
                        log.info("Scheduled cron automation '{}' with expression: {}",
                                config.getName(), schedule.getCronExpression());
                    } else {
                        log.error("Invalid cron expression for config {}: {}",
                                config.getName(), schedule.getCronExpression());
                        return;
                    }
                    break;

                default:
                    log.warn("Unknown schedule type: " + schedule.getType());
                    return;
            }

            if (scheduledTask != null) {
                scheduledTasks.put(config.getId(), scheduledTask);
            }

        } catch (Exception e) {
            log.error("Failed to schedule automation: " + config.getName(), e);
        }
    }

    public void unscheduleAutomation(Long configId) {
        ScheduledFuture<?> scheduledTask = scheduledTasks.remove(configId);
        if (scheduledTask != null) {
            scheduledTask.cancel(false);
            log.info("Unscheduled automation with id: " + configId);
        }
    }

    public void rescheduleAutomation(AutomationConfig config) {
        unscheduleAutomation(config.getId());
        if (config.isActive() && config.getSchedule() != null) {
            scheduleAutomation(config);
        }
    }

    public boolean isScheduled(Long configId) {
        return scheduledTasks.containsKey(configId) &&
                !scheduledTasks.get(configId).isCancelled();
    }

    public Map<Long, Boolean> getScheduledStatus() {
        Map<Long, Boolean> status = new ConcurrentHashMap<>();
        scheduledTasks.forEach((configId, future) -> {
            status.put(configId, !future.isCancelled() && !future.isDone());
        });
        return status;
    }

    private boolean isValidCronExpression(String cronExpression) {
        try {
            CronExpression.parse(cronExpression);
            return true;
        } catch (IllegalArgumentException e) {
            return false;
        }
    }
}
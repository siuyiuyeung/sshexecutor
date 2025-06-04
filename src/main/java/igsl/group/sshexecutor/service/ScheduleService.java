package igsl.group.sshexecutor.service;

import igsl.group.sshexecutor.entity.ExecutionResult;
import igsl.group.sshexecutor.entity.ScheduledTask;
import igsl.group.sshexecutor.repository.ScheduledTaskRepository;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.support.CronExpression;
import org.springframework.scheduling.support.CronTrigger;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ScheduledFuture;

@Service
@EnableScheduling
@Slf4j
public class ScheduleService {

    private final ScheduledTaskRepository scheduledTaskRepository;
    private final SshConfigService sshConfigService;
    private final TaskScheduler taskScheduler;
    private final Map<Long, ScheduledFuture<?>> scheduledJobs = new ConcurrentHashMap<>();

    public ScheduleService(ScheduledTaskRepository scheduledTaskRepository,
                           SshConfigService sshConfigService,
                           TaskScheduler taskScheduler) {
        this.scheduledTaskRepository = scheduledTaskRepository;
        this.sshConfigService = sshConfigService;
        this.taskScheduler = taskScheduler;
    }

    @PostConstruct
    public void initializeSchedules() {
        log.info("Initializing scheduled tasks...");
        List<ScheduledTask> activeTasks = scheduledTaskRepository.findByActive(true);
        for (ScheduledTask task : activeTasks) {
            try {
                scheduleTask(task);
                log.info("Scheduled task: {} with cron: {}", task.getName(), task.getCronExpression());
            } catch (Exception e) {
                log.error("Failed to schedule task: {}", task.getName(), e);
            }
        }
        log.info("Initialized {} scheduled tasks", activeTasks.size());
    }

    @PreDestroy
    public void shutdown() {
        log.info("Shutting down scheduled tasks...");
        scheduledJobs.values().forEach(future -> future.cancel(false));
        scheduledJobs.clear();
    }

    @Transactional
    public ScheduledTask createScheduledTask(ScheduledTask task) {
        // Validate cron expression
        validateCronExpression(task.getCronExpression());

        // Check for duplicate names
        if (scheduledTaskRepository.existsByName(task.getName())) {
            throw new IllegalArgumentException("Task with name '" + task.getName() + "' already exists");
        }

        // Calculate next run time
        task.setNextRunTime(calculateNextRunTime(task.getCronExpression()));

        // Save task
        ScheduledTask savedTask = scheduledTaskRepository.save(task);

        // Schedule if active
        if (task.isActive()) {
            scheduleTask(savedTask);
        }

        return savedTask;
    }

    @Transactional
    public ScheduledTask updateScheduledTask(Long id, ScheduledTask taskUpdate) {
        ScheduledTask existingTask = scheduledTaskRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Scheduled task not found with id: " + id));

        // Validate cron expression if changed
        if (!existingTask.getCronExpression().equals(taskUpdate.getCronExpression())) {
            validateCronExpression(taskUpdate.getCronExpression());
        }

        // Check for duplicate names
        if (!existingTask.getName().equals(taskUpdate.getName()) &&
                scheduledTaskRepository.existsByNameAndIdNot(taskUpdate.getName(), id)) {
            throw new IllegalArgumentException("Task with name '" + taskUpdate.getName() + "' already exists");
        }

        // Cancel existing schedule
        cancelScheduledTask(id);

        // Update fields
        existingTask.setName(taskUpdate.getName());
        existingTask.setSshConfig(taskUpdate.getSshConfig());
        existingTask.setCronExpression(taskUpdate.getCronExpression());
        existingTask.setDescription(taskUpdate.getDescription());
        existingTask.setActive(taskUpdate.isActive());
        existingTask.setNextRunTime(calculateNextRunTime(taskUpdate.getCronExpression()));

        ScheduledTask savedTask = scheduledTaskRepository.save(existingTask);

        // Reschedule if active
        if (savedTask.isActive()) {
            scheduleTask(savedTask);
        }

        return savedTask;
    }

    @Transactional
    public void deleteScheduledTask(Long id) {
        cancelScheduledTask(id);
        scheduledTaskRepository.deleteById(id);
    }

    @Transactional
    public void toggleTaskStatus(Long id) {
        ScheduledTask task = scheduledTaskRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Scheduled task not found with id: " + id));

        task.setActive(!task.isActive());

        if (task.isActive()) {
            task.setNextRunTime(calculateNextRunTime(task.getCronExpression()));
            scheduleTask(task);
        } else {
            task.setNextRunTime(null);
            cancelScheduledTask(id);
        }

        scheduledTaskRepository.save(task);
    }

    public List<ScheduledTask> getAllScheduledTasks() {
        return scheduledTaskRepository.findAll();
    }

    public ScheduledTask getScheduledTask(Long id) {
        return scheduledTaskRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Scheduled task not found with id: " + id));
    }

    @Transactional
    public void executeTaskManually(Long taskId) {
        ScheduledTask task = scheduledTaskRepository.findById(taskId)
                .orElseThrow(() -> new RuntimeException("Scheduled task not found with id: " + taskId));

        executeTask(task);
    }

    private void scheduleTask(ScheduledTask task) {
        Runnable runnable = () -> executeTask(task);

        ScheduledFuture<?> future = taskScheduler.schedule(
                runnable,
                new CronTrigger(task.getCronExpression())
        );

        scheduledJobs.put(task.getId(), future);
    }

    private void cancelScheduledTask(Long taskId) {
        ScheduledFuture<?> future = scheduledJobs.remove(taskId);
        if (future != null) {
            future.cancel(false);
        }
    }

    @Transactional
    public void executeTask(ScheduledTask task) {
        log.info("Executing scheduled task: {} for config: {}",
                task.getName(), task.getSshConfig().getName());

        try {
            // Execute SSH command
            ExecutionResult result = sshConfigService.executeConfig(task.getSshConfig().getId());

            // Update task statistics
            task.setLastRunTime(LocalDateTime.now());
            task.setRunCount(task.getRunCount() + 1);

            if (result.getSuccess()) {
                task.setSuccessCount(task.getSuccessCount() + 1);
                task.setLastRunStatus("SUCCESS");
            } else {
                task.setFailureCount(task.getFailureCount() + 1);
                task.setLastRunStatus("FAILED: " + result.getError());
            }

            // Calculate next run time
            if (task.isActive()) {
                task.setNextRunTime(calculateNextRunTime(task.getCronExpression()));
            }

            scheduledTaskRepository.save(task);

            log.info("Scheduled task {} completed with status: {}",
                    task.getName(), result.getSuccess() ? "SUCCESS" : "FAILED");

        } catch (Exception e) {
            log.error("Error executing scheduled task: {}", task.getName(), e);

            task.setLastRunTime(LocalDateTime.now());
            task.setRunCount(task.getRunCount() + 1);
            task.setFailureCount(task.getFailureCount() + 1);
            task.setLastRunStatus("ERROR: " + e.getMessage());

            if (task.isActive()) {
                task.setNextRunTime(calculateNextRunTime(task.getCronExpression()));
            }

            scheduledTaskRepository.save(task);
        }
    }

    private void validateCronExpression(String cronExpression) {
        try {
            CronExpression.parse(cronExpression);
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("Invalid cron expression: " + cronExpression, e);
        }
    }

    private LocalDateTime calculateNextRunTime(String cronExpression) {
        try {
            CronExpression cron = CronExpression.parse(cronExpression);
            return cron.next(LocalDateTime.now());
        } catch (Exception e) {
            log.error("Error calculating next run time for cron: {}", cronExpression, e);
            return null;
        }
    }

    public List<ScheduledTask> getTasksBySshConfig(Long sshConfigId) {
        return scheduledTaskRepository.findBySshConfigId(sshConfigId);
    }
}

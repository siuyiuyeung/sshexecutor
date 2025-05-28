package igsl.group.sshexecutor.service;

import igsl.group.sshexecutor.config.ExecutionConfig;
import igsl.group.sshexecutor.entity.ExecutionResult;
import igsl.group.sshexecutor.entity.SshConfig;
import igsl.group.sshexecutor.repository.ExecutionResultRepository;
import igsl.group.sshexecutor.repository.SshConfigRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

@Slf4j
@Service
public class SshConfigService {

    private final SshConfigRepository sshConfigRepository;
    private final ExecutionResultRepository executionResultRepository;
    private final SshService sshService;
    private final ExecutorService virtualThreadExecutor;
    private final Semaphore connectionSemaphore;
    private final ExecutionConfig executionConfig;

    public SshConfigService(SshConfigRepository sshConfigRepository,
                            ExecutionResultRepository executionResultRepository,
                            SshService sshService,
                            ExecutionConfig executionConfig) {
        this.sshConfigRepository = sshConfigRepository;
        this.executionResultRepository = executionResultRepository;
        this.sshService = sshService;
        this.executionConfig = executionConfig;

        // Create executor based on configuration
        if (executionConfig.isUseVirtualThreads()) {
            this.virtualThreadExecutor = Executors.newVirtualThreadPerTaskExecutor();
            log.info("Using virtual threads for SSH execution");
        } else {
            this.virtualThreadExecutor = Executors.newFixedThreadPool(
                    executionConfig.getMaxConcurrentConnections()
            );
            log.info("Using fixed thread pool with {} threads",
                    executionConfig.getMaxConcurrentConnections());
        }

        // Semaphore to limit concurrent SSH connections
        this.connectionSemaphore = new Semaphore(
                executionConfig.getMaxConcurrentConnections()
        );
    }

    public List<SshConfig> getAllConfigs() {
        return sshConfigRepository.findAll();
    }

    public Optional<SshConfig> getConfigById(Long id) {
        return sshConfigRepository.findById(id);
    }

    public Optional<SshConfig> getConfigByName(String name) {
        return sshConfigRepository.findByName(name);
    }

    @Transactional
    public SshConfig saveConfig(SshConfig config) {
        return sshConfigRepository.save(config);
    }

    @Transactional
    public void deleteConfig(Long id) {
        sshConfigRepository.deleteById(id);
    }

    public boolean configExists(String name) {
        return sshConfigRepository.existsByName(name);
    }

    @Transactional
    public ExecutionResult executeConfig(Long configId) {
        Optional<SshConfig> configOpt = sshConfigRepository.findById(configId);
        if (configOpt.isEmpty()) {
            throw new RuntimeException("Configuration not found with id: " + configId);
        }

        SshConfig config = configOpt.get();
        ExecutionResult result = sshService.executeCommand(config);
        return executionResultRepository.save(result);
    }

    @Transactional
    public List<ExecutionResult> executeAllConfigs() {
        List<SshConfig> configs = sshConfigRepository.findAll();
        log.info("Executing {} configurations in parallel", configs.size());

        // Create CompletableFutures for parallel execution using virtual threads
        List<CompletableFuture<ExecutionResult>> futures = configs.stream()
                .map(config -> CompletableFuture.supplyAsync(
                        () -> executeWithSemaphore(config),
                        virtualThreadExecutor
                ))
                .toList();

        // Wait for all executions to complete and collect results
        List<ExecutionResult> results = futures.stream()
                .map(this::handleFuture)
                .toList();

        log.info("Completed execution of {} configurations", results.size());
        return results;
    }

    @Transactional
    public List<ExecutionResult> executeSelectedConfigs(List<Long> configIds) {
        // Fetch all configs first to validate they exist
        List<SshConfig> configs = configIds.stream()
                .map(id -> sshConfigRepository.findById(id)
                        .orElseThrow(() -> new RuntimeException("Configuration not found with id: " + id)))
                .toList();

        log.info("Executing {} selected configurations in parallel", configs.size());

        // Execute in parallel using virtual threads
        List<CompletableFuture<ExecutionResult>> futures = configs.stream()
                .map(config -> CompletableFuture.supplyAsync(
                        () -> executeWithSemaphore(config),
                        virtualThreadExecutor
                ))
                .toList();

        // Wait for all executions to complete with timeout
        List<ExecutionResult> results = futures.stream()
                .map(this::handleFuture)
                .toList();

        log.info("Completed execution of {} selected configurations", results.size());
        return results;
    }

    private ExecutionResult executeWithSemaphore(SshConfig config) {
        try {
            // Acquire permit to limit concurrent connections
            connectionSemaphore.acquire();
            try {
                log.debug("Executing command for config: {}", config.getName());
                return executeAndSave(config);
            } finally {
                connectionSemaphore.release();
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return createFailedResult(config, "Execution interrupted: " + e.getMessage());
        }
    }

    private ExecutionResult executeAndSave(SshConfig config) {
        try {
            ExecutionResult result = sshService.executeCommand(config);
            // Save in a separate transaction to avoid locking issues
            return executionResultRepository.save(result);
        } catch (Exception e) {
            log.error("Failed to execute command for config: {}", config.getName(), e);
            // Create a failed result if execution fails
            return executionResultRepository.save(createFailedResult(config, e.getMessage()));
        }
    }

    private ExecutionResult createFailedResult(SshConfig config, String errorMessage) {
        return ExecutionResult.builder()
                .sshConfig(config)
                .command(config.getCommand() != null ? config.getCommand() : config.getScript())
                .output("")
                .error("Execution failed: " + errorMessage)
                .exitCode(-1)
                .success(false)
                .executionTime(0L)
                .build();
    }

    private ExecutionResult handleFuture(CompletableFuture<ExecutionResult> future) {
        try {
            // Wait with timeout from configuration
            return future.get(executionConfig.getExecutionTimeout(), TimeUnit.MILLISECONDS);
        } catch (TimeoutException e) {
            future.cancel(true);
            log.error("Execution timed out after {} ms", executionConfig.getExecutionTimeout());
            return createFailedResult(null, "Execution timed out");
        } catch (Exception e) {
            log.error("Error handling future", e);
            return createFailedResult(null, "Execution error: " + e.getMessage());
        }
    }

    public List<ExecutionResult> getExecutionHistory(Long configId) {
        Optional<SshConfig> configOpt = sshConfigRepository.findById(configId);
        if (configOpt.isEmpty()) {
            return List.of();
        }
        return executionResultRepository.findBySshConfigOrderByExecutedAtDesc(configOpt.get());
    }

    public List<ExecutionResult> getRecentExecutions() {
        return executionResultRepository.findTop10ByOrderByExecutedAtDesc();
    }

    public ExecutionResult getExecutionResult(Long id) {
        return executionResultRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Execution result not found with id: " + id));
    }

    public boolean testConnection(Long configId) {
        Optional<SshConfig> configOpt = sshConfigRepository.findById(configId);
        return configOpt.map(sshService::testConnection).orElse(false);
    }

    // Clean up executor on bean destruction
    public void shutdown() {
        log.info("Shutting down virtual thread executor");
        virtualThreadExecutor.shutdown();
        try {
            if (!virtualThreadExecutor.awaitTermination(10, TimeUnit.SECONDS)) {
                virtualThreadExecutor.shutdownNow();
            }
        } catch (InterruptedException e) {
            virtualThreadExecutor.shutdownNow();
            Thread.currentThread().interrupt();
        }
    }
}